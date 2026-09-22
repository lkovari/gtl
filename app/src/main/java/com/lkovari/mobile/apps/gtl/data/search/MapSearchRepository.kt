package com.lkovari.mobile.apps.gtl.data.search

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lkovari.mobile.apps.gtl.engine.IndexProgress
import com.lkovari.mobile.apps.gtl.engine.IndexResume
import com.lkovari.mobile.apps.gtl.engine.IndexResumeAction
import com.lkovari.mobile.apps.gtl.engine.MapPlaceKind
import com.lkovari.mobile.apps.gtl.engine.MapSearch
import com.lkovari.mobile.apps.gtl.engine.MapSearchCandidate
import com.lkovari.mobile.apps.gtl.engine.MapSearchHit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class MapIndexStatus(
    val indexing: Boolean = false,
    val ready: Boolean = false,
    val failed: Boolean = false,
    val truncated: Boolean = false
)

data class MapOrigin(
    val latitude: Double,
    val longitude: Double
)

class MapSearchRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = MapSearchDatabase.create(appContext).places()
    private val gate = Mutex()
    private val indexer = MapSearchIndexer(dao, gate)
    private val workManager = WorkManager.getInstance(appContext)
    private val generation = AtomicInteger(0)

    @Volatile
    private var activeKey: String? = null

    @Volatile
    private var activePath: String? = null

    @Volatile
    private var origin: MapOrigin? = null

    private val statusFlow = MutableStateFlow(MapIndexStatus())
    val status: StateFlow<MapIndexStatus> = statusFlow.asStateFlow()

    fun origin(): Pair<Double, Double>? {
        val current = origin ?: return null
        return current.latitude to current.longitude
    }

    suspend fun activate(file: File?) {
        withContext(Dispatchers.IO) {
            if (file == null || !file.isFile) {
                stopActive()
                return@withContext
            }
            val path = file.absolutePath
            val key = MapSearch.mapKey(path, file.length(), file.lastModified())
            if (path == activePath && key == activeKey) {
                val current = statusFlow.value
                if (current.indexing || current.ready) {
                    return@withContext
                }
            }
            val previous = activePath
            val previousKey = activeKey
            if (previous != null && previous != path) {
                generation.incrementAndGet()
                workManager.cancelUniqueWork(workName(previous))
            }
            val state = gate.withLock { dao.state(key) }
            if (state == null) {
                gate.withLock { dao.deletePath(path) }
            }
            val decision = IndexResume.action(state.toProgress(), System.currentTimeMillis())
            val sameFileNewKey = previous == path && previousKey != null && previousKey != key
            if (sameFileNewKey) {
                generation.incrementAndGet()
            }
            activeKey = key
            activePath = path
            origin = state?.let { MapOrigin(it.originLatitude, it.originLongitude) }
            val policy = if (sameFileNewKey) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
            when (decision) {
                IndexResumeAction.Ready -> {
                    statusFlow.value = MapIndexStatus(ready = true)
                    workManager.cancelUniqueWork(workName(path))
                }
                IndexResumeAction.Truncated -> {
                    statusFlow.value = MapIndexStatus(ready = true, truncated = true)
                    workManager.cancelUniqueWork(workName(path))
                }
                IndexResumeAction.Backoff -> {
                    statusFlow.value = MapIndexStatus(failed = true)
                    enqueue(path, generation.get(), policy)
                }
                IndexResumeAction.Resume -> {
                    statusFlow.value = MapIndexStatus(indexing = true)
                    enqueue(path, generation.get(), policy)
                }
            }
            gate.withLock { dao.deleteExcept(path) }
        }
    }

    suspend fun delete(file: File) {
        withContext(Dispatchers.IO) {
            val path = file.absolutePath
            workManager.cancelUniqueWork(workName(path))
            if (activePath == path) {
                generation.incrementAndGet()
                clearActive()
                statusFlow.value = MapIndexStatus()
            }
            gate.withLock { dao.deletePath(path) }
        }
    }

    suspend fun runIndex(file: File, expectedGeneration: Int): androidx.work.ListenableWorker.Result {
        if (!file.isFile) {
            return androidx.work.ListenableWorker.Result.failure()
        }
        val path = file.absolutePath
        val key = MapSearch.mapKey(path, file.length(), file.lastModified())
        if (generation.get() != expectedGeneration || activePath != path || activeKey != key) {
            return androidx.work.ListenableWorker.Result.success()
        }
        val state = gate.withLock { dao.state(key) }
        val decision = IndexResume.action(state.toProgress(), System.currentTimeMillis())
        when (decision) {
            IndexResumeAction.Ready -> {
                publishOrigin(state)
                statusFlow.value = MapIndexStatus(ready = true)
                return androidx.work.ListenableWorker.Result.success()
            }
            IndexResumeAction.Truncated -> {
                publishOrigin(state)
                statusFlow.value = MapIndexStatus(ready = true, truncated = true)
                return androidx.work.ListenableWorker.Result.success()
            }
            IndexResumeAction.Backoff -> {
                statusFlow.value = MapIndexStatus(failed = true)
                return androidx.work.ListenableWorker.Result.retry()
            }
            IndexResumeAction.Resume -> Unit
        }
        statusFlow.value = MapIndexStatus(indexing = true)
        return try {
            val outcome = indexer.index(file, key, { stillCurrent(path, key, expectedGeneration) }) { latitude, longitude ->
                if (stillCurrent(path, key, expectedGeneration)) {
                    origin = MapOrigin(latitude, longitude)
                }
            }
            if (!stillCurrent(path, key, expectedGeneration)) {
                return androidx.work.ListenableWorker.Result.success()
            }
            when (outcome) {
                IndexOutcome.Done -> statusFlow.value = MapIndexStatus(ready = true)
                IndexOutcome.Truncated -> statusFlow.value = MapIndexStatus(ready = true, truncated = true)
                IndexOutcome.Cancelled -> Unit
            }
            androidx.work.ListenableWorker.Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (stillCurrent(path, key, expectedGeneration)) {
                rememberFailure(file, key)
                statusFlow.value = MapIndexStatus(failed = true)
            }
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    suspend fun search(rawQuery: String, originLat: Double, originLon: Double): List<MapSearchHit> {
        val key = activeKey ?: return emptyList()
        val match = MapSearch.ftsMatch(rawQuery) ?: return emptyList()
        val rows = gate.withLock { dao.match(key, match, CandidateLimit) }
        return withContext(Dispatchers.Default) {
            val candidates = rows.mapNotNull { row ->
                val kind = kindOf(row.kind) ?: return@mapNotNull null
                MapSearchCandidate(
                    name = row.name,
                    nameFold = row.nameFold,
                    kind = kind,
                    latitude = row.latitude,
                    longitude = row.longitude
                )
            }
            MapSearch.rank(rawQuery, candidates, originLat, originLon)
        }
    }

    private fun enqueue(path: String, gen: Int, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<MapSearchIndexWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    MapSearchIndexWorker.KEY_PATH to path,
                    MapSearchIndexWorker.KEY_GENERATION to gen
                )
            )
            .build()
        workManager.enqueueUniqueWork(workName(path), policy, request)
    }

    private suspend fun rememberFailure(file: File, key: String) {
        gate.withLock {
            val previous = dao.state(key)
            val attempt = (previous?.attempt ?: 0) + 1
            val delayMillis = BACKOFF_SECONDS * 1000L shl attempt.coerceAtMost(6)
            dao.upsertState(
                MapIndexStateEntity(
                    mapKey = key,
                    path = file.absolutePath,
                    done = false,
                    truncated = false,
                    attempt = attempt,
                    nextAttemptAtMillis = System.currentTimeMillis() + delayMillis,
                    subIndex = previous?.subIndex ?: 0,
                    tileX = previous?.tileX ?: Long.MIN_VALUE,
                    tileY = previous?.tileY ?: Long.MIN_VALUE,
                    originLatitude = previous?.originLatitude ?: origin?.latitude ?: 0.0,
                    originLongitude = previous?.originLongitude ?: origin?.longitude ?: 0.0
                )
            )
        }
    }

    private fun publishOrigin(state: MapIndexStateEntity?) {
        if (state == null) {
            return
        }
        origin = MapOrigin(state.originLatitude, state.originLongitude)
    }

    private fun stillCurrent(path: String, key: String, expectedGeneration: Int): Boolean {
        return generation.get() == expectedGeneration && activePath == path && activeKey == key
    }

    private fun stopActive() {
        val path = activePath
        generation.incrementAndGet()
        if (path != null) {
            workManager.cancelUniqueWork(workName(path))
        }
        clearActive()
        statusFlow.value = MapIndexStatus()
    }

    private fun clearActive() {
        activeKey = null
        activePath = null
        origin = null
    }

    private fun workName(path: String): String {
        return "map-search-${path.length}-${path.hashCode()}"
    }

    private fun kindOf(name: String): MapPlaceKind? {
        return try {
            MapPlaceKind.valueOf(name)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun MapIndexStateEntity?.toProgress(): IndexProgress? {
        val state = this ?: return null
        return IndexProgress(
            done = state.done,
            truncated = state.truncated,
            nextAttemptAtMillis = state.nextAttemptAtMillis
        )
    }

    private companion object {
        const val CandidateLimit = 160
        const val BACKOFF_SECONDS = 30L
    }
}
