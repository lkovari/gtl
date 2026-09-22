package com.lkovari.mobile.apps.gtl.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.SQLException
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lkovari.mobile.apps.gtl.GtlApplication
import com.lkovari.mobile.apps.gtl.MainActivity
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.data.db.GpsEventEntity
import com.lkovari.mobile.apps.gtl.data.location.LocationClient
import com.lkovari.mobile.apps.gtl.data.prefs.GtlSettings
import com.lkovari.mobile.apps.gtl.data.sensor.AndroidBaroAltitude
import com.lkovari.mobile.apps.gtl.engine.BaroAltitude
import com.lkovari.mobile.apps.gtl.engine.BikeLeanAngle
import com.lkovari.mobile.apps.gtl.engine.EventKind
import com.lkovari.mobile.apps.gtl.engine.FixAcceptance
import com.lkovari.mobile.apps.gtl.engine.GpsQualityNotice
import com.lkovari.mobile.apps.gtl.engine.KalmanTrackFilter
import com.lkovari.mobile.apps.gtl.engine.TrackFix
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrackingForegroundService : LifecycleService() {
    private val recordingLock = Mutex()
    private val recordingGeneration = AtomicInteger(0)
    private var locationJob: Job? = null
    private var activeSessionId: Long? = null
    private var lastAccepted: TrackFix? = null
    private var lastFiltered: TrackFix? = null
    private var lastKind: EventKind = EventKind.START
    private var lastStoredAltitude: Double? = null
    private var lastStoredSpeed: Float? = null
    private var lastReportedSpeed: Float? = null
    private var sessionStartElapsed: Long = 0L
    private var lastAcceptElapsed: Long = 0L
    private var postedNotificationText: String? = null
    private var kalman = KalmanTrackFilter()
    private var autoCalibratedThisSession = false
    private var pendingCalibrationAltitude: Double? = null

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
        recordingGeneration.incrementAndGet()
        lifecycleScope.launch {
            recordingLock.withLock {
                if (locationJob != null) {
                    refreshNotification()
                    return@withLock
                }
                val settings = app.preferences.settings.first()
                val existing = app.trackRepository.openSession()
                val sessionId = existing?.id
                    ?: app.trackRepository.startSession(settings.usageType, settings.measurementSystem)
                val usageTypeName = existing?.usageType ?: settings.usageType.name
                activeSessionId = sessionId
                val latest = app.trackRepository.latestEvent(sessionId)
                lastAccepted = latest?.let { event ->
                    TrackFix(
                        timestampMillis = event.timestamp,
                        latitude = event.latitude,
                        longitude = event.longitude,
                        altitude = event.altitude ?: 0.0,
                        speedMps = event.speed ?: 0f,
                        bearing = event.bearing,
                        accuracyMeters = event.accuracy,
                        satellitesInFix = event.satellitesInFix
                    )
                }
                lastStoredAltitude = latest?.altitude
                lastStoredSpeed = latest?.speed
                lastReportedSpeed = latest?.speed
                sessionStartElapsed = SystemClock.elapsedRealtime()
                lastAcceptElapsed = if (latest == null) 0L else sessionStartElapsed
                kalman = KalmanTrackFilter()
                lastFiltered = null
                autoCalibratedThisSession = false
                pendingCalibrationAltitude = null
                val accepted = lastAccepted
                if (accepted != null) {
                    kalman.seedFrom(accepted)
                    lastFiltered = accepted
                }
                lastKind = if (lastAccepted == null) EventKind.START else EventKind.MOVE
                val acceptedCount = if (lastAccepted == null) 0 else 1
                app.trackingState.update {
                    it.copy(
                        logging = true,
                        sessionId = sessionId,
                        acceptedFixCount = acceptedCount,
                        rejectedFixCount = 0,
                        poorGps = false,
                        loggingError = false,
                        gpsOff = false,
                        temperatureAvailable = app.ambientTemperatureSource.isAvailable,
                        pressureAvailable = app.pressureSource.isAvailable
                    )
                }
                refreshNotification()
                locationJob = lifecycleScope.launch {
                    launch { collectLocation(app, sessionId, settings, usageTypeName) }
                    launch { watchGpsQuality(app) }
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
                        app.compassSource.samples().collectLatest { sample ->
                            app.trackingState.update {
                                it.copy(
                                    azimuthDegrees = sample.azimuthDegrees,
                                    compassAccuracy = sample.accuracy
                                )
                            }
                        }
                    }
                    launch {
                        combine(
                            app.pressureSource.pressures(),
                            app.preferences.settings
                        ) { value, prefs ->
                            Triple(value, prefs.qnhHpa, prefs.baroPressureOffsetHpa)
                        }.collectLatest { (value, qnh, offset) ->
                            app.trackingState.update {
                                it.copy(
                                    pressureHpa = value,
                                    baroAltitude = AndroidBaroAltitude.metersFromPressureHpa(value, qnh, offset),
                                    pressureAvailable = true
                                )
                            }
                        }
                    }
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
        if (settings.gnssOnly && !client.isGpsProviderEnabled()) {
            app.trackingState.update { it.copy(gpsOff = true) }
            refreshNotification()
        }
        client.locations(
            settings.minTimeMillis.coerceAtLeast(500L),
            0f,
            settings.gnssOnly,
            recording = true
        ).collect { location ->
            val gnss = app.trackingState.state.value.gnss
            val reportedSpeed = if (location.hasSpeed()) location.speed else null
            if (reportedSpeed != null) {
                lastReportedSpeed = reportedSpeed
            }
            val speedForFilter = reportedSpeed ?: lastReportedSpeed ?: 0f
            val fix = TrackFix(
                timestampMillis = location.time,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speedMps = speedForFilter,
                bearing = location.bearing,
                accuracyMeters = location.accuracy,
                satellitesInFix = gnss?.satellitesInFix ?: 0
            )
            val wasGpsOff = app.trackingState.state.value.gpsOff
            app.trackingState.update {
                it.copy(lastLocation = location, provider = location.provider, gpsOff = false)
            }
            if (wasGpsOff) {
                refreshNotification()
            }
            val filter = settings.toFilter()
            if (!FixAcceptance.hasUsableAccuracy(location.hasAccuracy(), location.accuracy)) {
                noteRejected(app)
                return@collect
            }
            if (fix.accuracyMeters > filter.minAccuracyMeters) {
                noteRejected(app)
                return@collect
            }
            if (fix.satellitesInFix < filter.minSatellites) {
                noteRejected(app)
                return@collect
            }
            if (location.hasAltitude()) {
                maybeAutoCalibrateBaro(app, settings, fix.altitude)
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
            if (!FixAcceptance.shouldAccept(
                    lastAccepted,
                    forStore,
                    filter,
                    settings.recordingDensityValue,
                    settings.usageType
                )
            ) {
                noteRejected(app)
                return@collect
            }
            val kind = when {
                lastAccepted == null -> EventKind.START
                !location.hasSpeed() -> EventKind.MOVE
                forStore.speedMps < settings.usageType.pauseSpeedMps() -> EventKind.PAUSE
                else -> EventKind.MOVE
            }
            val storedAltitude = if (location.hasAltitude()) forStore.altitude else null
            val storedSpeed = if (location.hasSpeed()) forStore.speedMps else null
                val live = app.trackingState.state.value
                val temp = if (live.temperatureAvailable) live.temperatureCelsius else null
                try {
                    app.trackRepository.insertEvent(
                        GpsEventEntity(
                            sessionId = sessionId,
                            timestamp = forStore.timestampMillis,
                            latitude = forStore.latitude,
                            longitude = forStore.longitude,
                            altitude = storedAltitude,
                            speed = storedSpeed,
                            bearing = forStore.bearing,
                            accuracy = forStore.accuracyMeters,
                            satellitesInFix = forStore.satellitesInFix,
                            ambientTemperature = temp,
                            accelX = live.accel?.getOrNull(0),
                            accelY = live.accel?.getOrNull(1),
                            accelZ = live.accel?.getOrNull(2),
                            leanAngle = live.leanAngle,
                            usageType = usageTypeName,
                            isPlacemark = kind != EventKind.MOVE,
                            eventKind = kind.name,
                            baroAltitude = live.baroAltitude,
                            pressureHpa = live.pressureHpa
                        )
                    )
                } catch (_: SQLException) {
                    app.trackingState.update { it.copy(loggingError = true) }
                    stopRecording()
                    return@collect
                }
                lastAccepted = forStore
                lastStoredAltitude = storedAltitude
                lastStoredSpeed = storedSpeed
                lastAcceptElapsed = SystemClock.elapsedRealtime()
                lastKind = kind
                val nextCount = live.acceptedFixCount + 1
                app.trackingState.update { it.copy(acceptedFixCount = nextCount, poorGps = false) }
                refreshNotification()
        }
    }

    private suspend fun watchGpsQuality(app: GtlApplication) {
        while (true) {
            delay(5_000)
            val anchor = if (lastAcceptElapsed > 0L) lastAcceptElapsed else sessionStartElapsed
            if (anchor == 0L) {
                continue
            }
            val poor = GpsQualityNotice.isPoor(SystemClock.elapsedRealtime() - anchor)
            if (app.trackingState.state.value.poorGps == poor) {
                continue
            }
            app.trackingState.update { it.copy(poorGps = poor) }
            refreshNotification()
        }
    }

    private fun noteRejected(app: GtlApplication) {
        app.trackingState.update { it.copy(rejectedFixCount = it.rejectedFixCount + 1) }
    }

    private suspend fun maybeAutoCalibrateBaro(app: GtlApplication, settings: GtlSettings, gpsAltitudeMeters: Double) {
        if (autoCalibratedThisSession || !settings.autoCalibrateBaroEnabled) {
            return
        }
        val pressure = app.trackingState.state.value.pressureHpa
        val previousAltitude = pendingCalibrationAltitude
        pendingCalibrationAltitude = gpsAltitudeMeters
        val eligible = BaroAltitude.autoCalibrateEligible(
            pressure,
            gpsAltitudeMeters,
            autoCalibratedThisSession,
            settings.autoCalibrateBaroEnabled,
            previousAltitude
        )
        if (!eligible || pressure == null || !BaroAltitude.isPlausiblePressureHpa(pressure)) {
            return
        }
        val offset = BaroAltitude.offsetHpa(pressure, gpsAltitudeMeters, settings.qnhHpa)
        app.preferences.setBaroPressureOffsetHpa(offset)
        autoCalibratedThisSession = true
    }

    private fun stopRecording() {
        val app = application as GtlApplication
        val stopGen = recordingGeneration.get()
        lifecycleScope.launch {
            recordingLock.withLock {
                val sessionId = activeSessionId
                val live = app.trackingState.state.value
                val last = live.lastLocation
                val filtered = lastFiltered
                val accepted = lastAccepted
                locationJob?.cancel()
                locationJob = null
                if (sessionId != null) {
                    val usageTypeName = app.trackRepository.openSession()?.usageType
                        ?: app.preferences.settings.first().usageType.name
                    val temp = if (live.temperatureAvailable) live.temperatureCelsius else null
                    val stopAltitude = lastStoredAltitude ?: last?.let { location ->
                        if (location.hasAltitude()) location.altitude else null
                    }
                    val stopFix = accepted ?: filtered ?: last?.let { location ->
                        TrackFix(
                            timestampMillis = location.time,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            speedMps = location.speed,
                            bearing = location.bearing,
                            accuracyMeters = location.accuracy,
                            satellitesInFix = live.gnss?.satellitesInFix ?: 0
                        )
                    }
                    if (stopFix != null) {
                        try {
                            app.trackRepository.insertEvent(
                                GpsEventEntity(
                                    sessionId = sessionId,
                                    timestamp = stopFix.timestampMillis,
                                    latitude = stopFix.latitude,
                                    longitude = stopFix.longitude,
                                    altitude = stopAltitude,
                                    speed = lastStoredSpeed,
                                    bearing = stopFix.bearing,
                                    accuracy = stopFix.accuracyMeters,
                                    satellitesInFix = stopFix.satellitesInFix,
                                    ambientTemperature = temp,
                                    accelX = live.accel?.getOrNull(0),
                                    accelY = live.accel?.getOrNull(1),
                                    accelZ = live.accel?.getOrNull(2),
                                    leanAngle = live.leanAngle,
                                    usageType = usageTypeName,
                                    isPlacemark = true,
                                    eventKind = EventKind.STOP.name,
                                    baroAltitude = live.baroAltitude,
                                    pressureHpa = live.pressureHpa
                                )
                            )
                        } catch (_: SQLException) {
                        }
                    }
                    app.trackRepository.stopSession(sessionId)
                }
                activeSessionId = null
                if (recordingGeneration.get() != stopGen) {
                    return@withLock
                }
                app.trackingState.update {
                    LiveTrackingState(
                        loggingError = live.loggingError,
                        temperatureAvailable = app.ambientTemperatureSource.isAvailable,
                        pressureAvailable = app.pressureSource.isAvailable
                    )
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun startAsForeground() {
        val text = getString(R.string.status_waiting_gps)
        postedNotificationText = text
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun refreshNotification() {
        val live = (application as GtlApplication).trackingState.state.value
        val text = when {
            live.gpsOff -> getString(R.string.status_gps_off)
            live.poorGps -> getString(R.string.status_poor_gps)
            live.acceptedFixCount == 0 -> getString(R.string.status_waiting_gps)
            else -> getString(R.string.notification_text)
        }
        if (text == postedNotificationText) {
            return
        }
        postedNotificationText = text
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
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
            .setContentText(text)
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
