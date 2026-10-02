package com.aozijx.passly.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.TelemetryRuntime
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

internal enum class DeviceLockTrigger {
    SCREEN_OFF,
    FLIP,
}

internal data class DeviceLockResult(
    val locked: Boolean,
    val shouldClearTask: Boolean = false,
)

/**
 * Tracks screen-off work across the suspend boundary used to seal the vault.
 * A generation prevents an older completion from clearing a newer screen-off event.
 */
internal class ScreenOffLockTracker {
    private val generationCounter = AtomicLong(NO_PENDING_GENERATION)
    private val pendingGeneration = AtomicLong(NO_PENDING_GENERATION)

    fun markScreenOff(): Long = generationCounter.incrementAndGet().also(pendingGeneration::set)

    fun pendingGeneration(): Long? = pendingGeneration.get().takeIf { it != NO_PENDING_GENERATION }

    fun complete(completedGeneration: Long) {
        pendingGeneration.compareAndSet(completedGeneration, NO_PENDING_GENERATION)
    }

    private companion object {
        const val NO_PENDING_GENERATION = 0L
    }
}

internal class DeviceLockHandler(
    private val sessionAccessState: SecureSessionAccessState,
    private val sessionLockController: SessionLockController,
    private val securitySettings: StateFlow<SecuritySettings>,
) {
    suspend fun handle(trigger: DeviceLockTrigger): DeviceLockResult {
        val security = securitySettings.value
        val reason = when (trigger) {
            DeviceLockTrigger.SCREEN_OFF -> LockReason.BACKGROUND
            DeviceLockTrigger.FLIP -> {
                if (!sessionAccessState.isUnlocked() && !sessionAccessState.isRecoveryMode()) {
                    return DeviceLockResult(locked = false)
                }
                if (!security.isFlipToLockEnabled) return DeviceLockResult(locked = false)
                LockReason.USER
            }
        }
        sessionLockController.lock(reason)
        return DeviceLockResult(
            locked = true,
            shouldClearTask = security.isFlipToLockEnabled &&
                security.isFlipExitAndClearStackEnabled,
        )
    }
}

/** Owns process-level screen-off reception and foreground-only flip sensing. */
@Singleton
class DeviceLockController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    sessionAccessState: SecureSessionAccessState,
    sessionLockController: SessionLockController,
    securitySettingsSource: SecuritySettingsSource,
) : SensorEventListener {
    private val lockScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val triggerMutex = Mutex()
    private val securitySettings = securitySettingsSource.security.stateIn(
        scope = mainScope,
        started = SharingStarted.Eagerly,
        initialValue = SecuritySettings(),
    )
    private val handler = DeviceLockHandler(
        sessionAccessState = sessionAccessState,
        sessionLockController = sessionLockController,
        securitySettings = securitySettings,
    )
    private val clearTaskChannel = Channel<Unit>(Channel.BUFFERED)
    val clearTaskRequests: Flow<Unit> = clearTaskChannel.receiveAsFlow()
    private val screenOffLockTracker = ScreenOffLockTracker()
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var receiverStarted = false
    private var appInForeground = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_OFF) return
            val generation = screenOffLockTracker.markScreenOff()
            val pendingResult = goAsync()
            dispatch(
                trigger = DeviceLockTrigger.SCREEN_OFF,
                screenOffGeneration = generation,
                onComplete = pendingResult::finish,
            )
        }
    }

    init {
        mainScope.launch {
            securitySettings
                .map { settings -> settings.isFlipToLockEnabled }
                .distinctUntilChanged()
                .collect { updateSensorRegistration() }
        }
    }

    fun start() {
        if (receiverStarted) return
        receiverStarted = true
        ContextCompat.registerReceiver(
            context,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    fun onAppForeground() {
        appInForeground = true
        screenOffLockTracker.pendingGeneration()?.let { generation ->
            dispatch(
                trigger = DeviceLockTrigger.SCREEN_OFF,
                screenOffGeneration = generation,
            )
        }
        updateSensorRegistration()
    }

    fun onAppBackground() {
        appInForeground = false
        sensorManager.unregisterListener(this)
        if (!powerManager.isInteractive) {
            val generation = screenOffLockTracker.markScreenOff()
            dispatch(
                trigger = DeviceLockTrigger.SCREEN_OFF,
                screenOffGeneration = generation,
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        if (event.values[2] < FLIP_THRESHOLD) dispatch(DeviceLockTrigger.FLIP)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateSensorRegistration() {
        sensorManager.unregisterListener(this)
        if (!appInForeground || !securitySettings.value.isFlipToLockEnabled) return
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun dispatch(
        trigger: DeviceLockTrigger,
        screenOffGeneration: Long? = null,
        onComplete: () -> Unit = {},
    ) {
        lockScope.launch {
            try {
                triggerMutex.withLock {
                    val result = handler.handle(trigger)
                    screenOffGeneration?.let(screenOffLockTracker::complete)
                    TelemetryRuntime.i(
                        EventCategory.APPLICATION,
                        "device_lock.completed trigger=${trigger.name} locked=${result.locked} clear_task=${result.shouldClearTask}",
                    )
                    if (result.shouldClearTask) clearTaskChannel.send(Unit)
                }
            } finally {
                onComplete()
            }
        }
    }

    private companion object {
        const val FLIP_THRESHOLD = -8.5f
    }
}
