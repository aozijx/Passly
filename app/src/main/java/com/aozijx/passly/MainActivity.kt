package com.aozijx.passly

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.platform.permission.PermissionServices
import com.aozijx.passly.app.DeviceLockController
import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.request.PermissionRequestArbiter
import com.aozijx.passly.presentation.PasslyApp
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
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
    lateinit var deviceLockController: DeviceLockController

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
                authenticationHostRegistry = authenticationHostRegistry,
                noticePublisher = noticePublisher,
                permissionServices = permissionServices,
            )
        }

        lifecycleScope.launch {
            deviceLockController.clearTaskRequests.collect {
                closeAndClearTask()
            }
        }
    }

    private fun closeAndClearTask() {
        noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER))
        window.decorView.postDelayed(::finishAndRemoveTask, APP_CLOSE_MESSAGE_DELAY_MS)
    }

    private companion object {
        const val APP_CLOSE_MESSAGE_DELAY_MS = 1_000L
    }
}
