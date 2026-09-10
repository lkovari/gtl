package com.lkovari.mobile.apps.gtl.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lkovari.mobile.apps.gtl.GtlApplication
import com.lkovari.mobile.apps.gtl.MainActivity
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.data.db.GpsEventEntity
import com.lkovari.mobile.apps.gtl.data.gnss.GnssStatusSource
import com.lkovari.mobile.apps.gtl.data.location.LocationClient
import com.lkovari.mobile.apps.gtl.data.prefs.GtlSettings
import com.lkovari.mobile.apps.gtl.data.sensor.AccelerometerSource
import com.lkovari.mobile.apps.gtl.data.sensor.AmbientTemperatureSource
import com.lkovari.mobile.apps.gtl.data.sensor.CompassSource
import com.lkovari.mobile.apps.gtl.engine.BikeLeanAngle
import com.lkovari.mobile.apps.gtl.engine.EventKind
import com.lkovari.mobile.apps.gtl.engine.FixAcceptance
import com.lkovari.mobile.apps.gtl.engine.KalmanTrackFilter
import com.lkovari.mobile.apps.gtl.engine.TrackFix
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrackingForegroundService : LifecycleService() {
    private var locationJob: Job? = null
    private var lastAccepted: TrackFix? = null
    private var lastFiltered: TrackFix? = null
    private var lastKind: EventKind = EventKind.START
    private var kalman = KalmanTrackFilter()
    private var smoothingEnabled = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                stopRecording()
                return START_NOT_STICKY
            }
            else -> startRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        val app = application as GtlApplication
        startAsForeground()
        if (locationJob != null) {
            return
        }
        locationJob = lifecycleScope.launch {
            val settings = app.preferences.settings.first()
            val existing = app.trackRepository.openSession()
            val sessionId = existing?.id
                ?: app.trackRepository.startSession(settings.usageType, settings.measurementSystem)
            val usageTypeName = existing?.usageType ?: settings.usageType.name
            app.trackingState.update {
                it.copy(logging = true, sessionId = sessionId, temperatureAvailable = app.ambientTemperatureSource.isAvailable)
            }
            lastAccepted = app.trackRepository.latestEvent(sessionId)?.let { event ->
                TrackFix(
                    timestampMillis = event.timestamp,
                    latitude = event.latitude,
                    longitude = event.longitude,
                    altitude = event.altitude,
                    speedMps = event.speed,
                    bearing = event.bearing,
                    accuracyMeters = event.accuracy,
                    satellitesInFix = event.satellitesInFix
                )
            }
            kalman = KalmanTrackFilter()
            lastFiltered = null
            smoothingEnabled = settings.trackSmoothingEnabled
            val accepted = lastAccepted
            if (accepted != null) {
                kalman.seedFrom(accepted)
                lastFiltered = accepted
            }
            lastKind = if (lastAccepted == null) EventKind.START else EventKind.MOVE
            launch { collectLocation(app, sessionId, settings, usageTypeName) }
            launch {
                app.gnssStatusSource.snapshots().collectLatest { snapshot ->
                    app.trackingState.update { it.copy(gnss = snapshot) }
                }
            }
            launch {
                app.ambientTemperatureSource.temperatures().collectLatest { value ->
                    app.trackingState.update { it.copy(temperatureCelsius = value, temperatureAvailable = true) }
                }
            }
            launch {
                app.accelerometerSource.accelerations().collectLatest { value ->
                    app.trackingState.update { it.copy(accel = value) }
                }
            }
            launch {
                app.gravitySource.gravity().collectLatest { value ->
                    app.trackingState.update {
                        it.copy(leanAngle = BikeLeanAngle.fromGravity(value[0], value[1], value[2]))
                    }
                }
            }
            launch {
                app.compassSource.azimuthDegrees().collectLatest { value ->
                    app.trackingState.update { it.copy(azimuthDegrees = value) }
                }
            }
        }
    }

    private suspend fun collectLocation(
        app: GtlApplication,
        sessionId: Long,
        settings: GtlSettings,
        usageTypeName: String
    ) {
        val client = LocationClient(this)
        client.locations(settings.minTimeMillis.coerceAtLeast(500L), 0f, settings.gnssOnly).collect { location ->
            val gnss = app.trackingState.state.value.gnss
            val fix = TrackFix(
                timestampMillis = location.time,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speedMps = location.speed,
                bearing = location.bearing,
                accuracyMeters = location.accuracy,
                satellitesInFix = gnss?.satellitesInFix ?: 0
            )
            app.trackingState.update {
                it.copy(lastLocation = location, provider = location.provider)
            }
            val filter = settings.toFilter()
            if (fix.accuracyMeters > filter.minAccuracyMeters) {
                return@collect
            }
            if (fix.satellitesInFix < filter.minSatellites) {
                return@collect
            }
            val forStore = if (settings.trackSmoothingEnabled) {
                val filtered = kalman.observe(
                    fix,
                    settings.usageType,
                    settings.smoothingStrengthValue,
                    settings.stationaryLockEnabled
                )
                lastFiltered = filtered
                filtered
            } else {
                fix
            }
            if (FixAcceptance.shouldAccept(
                    lastAccepted,
                    forStore,
                    filter,
                    settings.recordingDensityValue,
                    settings.usageType
                )
            ) {
                val kind = when {
                    lastAccepted == null -> EventKind.START
                    forStore.speedMps < settings.usageType.pauseSpeedMps() -> EventKind.PAUSE
                    else -> EventKind.MOVE
                }
                val live = app.trackingState.state.value
                app.trackRepository.insertEvent(
                    GpsEventEntity(
                        sessionId = sessionId,
                        timestamp = forStore.timestampMillis,
                        latitude = forStore.latitude,
                        longitude = forStore.longitude,
                        altitude = forStore.altitude,
                        speed = forStore.speedMps,
                        bearing = forStore.bearing,
                        accuracy = forStore.accuracyMeters,
                        satellitesInFix = forStore.satellitesInFix,
                        ambientTemperature = live.temperatureCelsius,
                        accelX = live.accel?.getOrNull(0),
                        accelY = live.accel?.getOrNull(1),
                        accelZ = live.accel?.getOrNull(2),
                        leanAngle = live.leanAngle,
                        usageType = usageTypeName,
                        isPlacemark = kind != EventKind.MOVE,
                        eventKind = kind.name
                    )
                )
                lastAccepted = forStore
                lastKind = kind
            }
        }
    }

    private fun stopRecording() {
        locationJob?.cancel()
        locationJob = null
        val app = application as GtlApplication
        lifecycleScope.launch {
            val sessionId = app.trackingState.state.value.sessionId
            if (sessionId != null) {
                val live = app.trackingState.state.value
                val last = live.lastLocation
                val filtered = lastFiltered
                val usageTypeName = app.trackRepository.openSession()?.usageType
                    ?: app.preferences.settings.first().usageType.name
                if (smoothingEnabled && filtered != null) {
                    app.trackRepository.insertEvent(
                        GpsEventEntity(
                            sessionId = sessionId,
                            timestamp = System.currentTimeMillis(),
                            latitude = filtered.latitude,
                            longitude = filtered.longitude,
                            altitude = filtered.altitude,
                            speed = filtered.speedMps,
                            bearing = filtered.bearing,
                            accuracy = filtered.accuracyMeters,
                            satellitesInFix = live.gnss?.satellitesInFix ?: 0,
                            ambientTemperature = live.temperatureCelsius,
                            accelX = live.accel?.getOrNull(0),
                            accelY = live.accel?.getOrNull(1),
                            accelZ = live.accel?.getOrNull(2),
                            leanAngle = live.leanAngle,
                            usageType = usageTypeName,
                            isPlacemark = true,
                            eventKind = EventKind.STOP.name
                        )
                    )
                } else if (last != null) {
                    app.trackRepository.insertEvent(
                        GpsEventEntity(
                            sessionId = sessionId,
                            timestamp = System.currentTimeMillis(),
                            latitude = last.latitude,
                            longitude = last.longitude,
                            altitude = last.altitude,
                            speed = last.speed,
                            bearing = last.bearing,
                            accuracy = last.accuracy,
                            satellitesInFix = live.gnss?.satellitesInFix ?: 0,
                            ambientTemperature = live.temperatureCelsius,
                            accelX = live.accel?.getOrNull(0),
                            accelY = live.accel?.getOrNull(1),
                            accelZ = live.accel?.getOrNull(2),
                            leanAngle = live.leanAngle,
                            usageType = usageTypeName,
                            isPlacemark = true,
                            eventKind = EventKind.STOP.name
                        )
                    )
                }
                app.trackRepository.stopSession(sessionId)
            }
            app.trackingState.update { LiveTrackingState(temperatureAvailable = app.ambientTemperatureSource.isAvailable) }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, TrackingForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(launch)
            .setOngoing(true)
            .addAction(0, getString(R.string.action_stop), stop)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_STOP = "com.lkovari.mobile.apps.gtl.STOP_TRACKING"
        private const val CHANNEL_ID = "gtl_tracking"
        private const val NOTIFICATION_ID = 17
    }
}
