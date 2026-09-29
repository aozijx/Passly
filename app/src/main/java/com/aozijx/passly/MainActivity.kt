package com.aozijx.passly

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.platform.permission.PermissionServices
import com.aozijx.passly.app.ScreenOffLockController
import com.aozijx.passly.app.shell.FlipLockTriggerController
import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.request.PermissionRequestArbiter
import com.aozijx.passly.presentation.PasslyApp
import com.aozijx.passly.presentation.feature.shell.AppShellUiAction
import com.aozijx.passly.presentation.feature.shell.AppShellViewModel
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: AppShellViewModel by viewModels()

    @Inject
    lateinit var authenticationHostRegistry: AuthenticationHostRegistry

    @Inject
    lateinit var noticePublisher: AppNoticePublisher

    @Inject
    lateinit var permissionStatusReader: PermissionStatusReader

    @Inject
    lateinit var permissionRequestArbiter: PermissionRequestArbiter

    @Inject
    lateinit var permissionRequestHistory: PermissionRequestHistory

    @Inject
    lateinit var screenOffLockController: ScreenOffLockController

    private val flipLockTriggerController: FlipLockTriggerController by lazy {
        FlipLockTriggerController(this) {
            val state = viewModel.uiState.value
            if (!state.isAuthorized || !state.windowPolicy.isFlipToLockEnabled) {
                return@FlipLockTriggerController
            }
            viewModel.onAction(AppShellUiAction.LockFromFlip)
            if (state.windowPolicy.isFlipExitAndClearStackEnabled) closeAndClearTask()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppContentTheme)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val permissionServices = PermissionServices(
            statusReader = permissionStatusReader,
            requestArbiter = permissionRequestArbiter,
            requestHistory = permissionRequestHistory,
        )
        setContent {
            PasslyApp(
                activity = this,
                shellViewModel = viewModel,
                authenticationHostRegistry = authenticationHostRegistry,
                noticePublisher = noticePublisher,
                permissionServices = permissionServices,
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { it.windowPolicy.isFlipToLockEnabled }
                    .distinctUntilChanged()
                    .collect(flipLockTriggerController::setEnabled)
            }
        }
        lifecycleScope.launch {
            screenOffLockController.clearTaskRequests.collect {
                closeAndClearTask()
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.onAction(AppShellUiAction.UpdateInteraction)
    }

    override fun onStart() {
        super.onStart()
        flipLockTriggerController.start()
    }

    override fun onStop() {
        flipLockTriggerController.stop()
        super.onStop()
    }

    private fun closeAndClearTask() {
        noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER))
        window.decorView.postDelayed(::finishAndRemoveTask, APP_CLOSE_MESSAGE_DELAY_MS)
    }

    private companion object {
        const val APP_CLOSE_MESSAGE_DELAY_MS = 1_000L
    }
}
