package com.aozijx.passly.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aozijx.passly.R
import com.aozijx.passly.app.ActiveTaskClearRequests
import com.aozijx.passly.app.TaskClearActivityOwner
import com.aozijx.passly.app.ViewTaskClearCallbackScheduler
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.platform.permission.PermissionServices
import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.request.PermissionRequestArbiter
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var noticePublisher: AppNoticePublisher

    @Inject
    lateinit var permissionStatusReader: PermissionStatusReader

    @Inject
    lateinit var permissionRequestArbiter: PermissionRequestArbiter

    @Inject
    lateinit var permissionRequestHistory: PermissionRequestHistory

    @Inject
    lateinit var activeTaskClearRequests: ActiveTaskClearRequests

    private lateinit var taskClearActivityOwner: TaskClearActivityOwner

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppContentTheme)
        super.onCreate(savedInstanceState)
        taskClearActivityOwner =
            TaskClearActivityOwner(
                requests = activeTaskClearRequests,
                scheduler = ViewTaskClearCallbackScheduler(window.decorView),
                showReminder = { noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER)) },
                closeTask = ::finishAndRemoveTask,
            )
        excludeFromAutofill()
        enableEdgeToEdge()

        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val permissionServices =
            PermissionServices(
                statusReader = permissionStatusReader,
                requestArbiter = permissionRequestArbiter,
                requestHistory = permissionRequestHistory,
            )
        setContent {
            PasslyApp(
                activity = this,
                noticePublisher = noticePublisher,
                permissionServices = permissionServices,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        taskClearActivityOwner.onResumed()
    }

    override fun onPause() {
        taskClearActivityOwner.onPaused()
        super.onPause()
    }
}
