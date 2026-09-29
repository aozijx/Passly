package com.aozijx.passly.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal data class ScreenOffLockResult(
    val locked: Boolean,
    val shouldClearTask: Boolean = false,
)

internal class ScreenOffLockHandler(
    private val sessionAccessState: SecureSessionAccessState,
    private val sessionLockController: SessionLockController,
    private val securitySettingsSource: SecuritySettingsSource,
) {
    suspend fun handle(): ScreenOffLockResult {
        if (!sessionAccessState.isUnlocked() && !sessionAccessState.isRecoveryMode()) {
            return ScreenOffLockResult(locked = false)
        }

        sessionLockController.lock(LockReason.BACKGROUND)
        val security = securitySettingsSource.security.first()
        return ScreenOffLockResult(
            locked = true,
            shouldClearTask = security.isFlipToLockEnabled &&
                security.isFlipExitAndClearStackEnabled,
        )
    }
}

@Singleton
class ScreenOffLockController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    sessionAccessState: SecureSessionAccessState,
    sessionLockController: SessionLockController,
    securitySettingsSource: SecuritySettingsSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = ScreenOffLockHandler(
        sessionAccessState = sessionAccessState,
        sessionLockController = sessionLockController,
        securitySettingsSource = securitySettingsSource,
    )
    private val clearTaskChannel = Channel<Unit>(Channel.BUFFERED)
    val clearTaskRequests: Flow<Unit> = clearTaskChannel.receiveAsFlow()
    private var started = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_OFF) return
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val result = handler.handle()
                    if (result.shouldClearTask) clearTaskChannel.send(Unit)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    fun start() {
        if (started) return
        started = true
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }
}
