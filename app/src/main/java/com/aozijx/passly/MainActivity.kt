package com.aozijx.passly

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aozijx.passly.core.message.AppMessageCategory
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.core.permission.ActivityPermissionRequester
import com.aozijx.passly.core.permission.AppPermission
import com.aozijx.passly.core.permission.AppPermissionManager
import com.aozijx.passly.feature.backup.BackupCoordinator
import com.aozijx.passly.feature.main.MainSensorController
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.main.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var backupCoordinator: BackupCoordinator

    @Inject
    lateinit var permissionManager: AppPermissionManager

    private val sensorController: MainSensorController by lazy {
        MainSensorController(this) {
            if (viewModel.isAuthorizedNow()) {
                viewModel.handleIntent(MainIntent.Lock)
                if (sensorController.isFlipExitAndClearStackEnabled) {
                    AppMessageCenter.publish(
                        text = "应用即将关闭",
                        category = AppMessageCategory.APP_CLOSE
                    )
                    window.decorView.postDelayed({
                        finishAndRemoveTask()
                        exitProcess(0)
                    }, APP_CLOSE_MESSAGE_DELAY_MS)
                }
            }
        }
    }

    private val permissionRequester: ActivityPermissionRequester by lazy {
        ActivityPermissionRequester(this, permissionManager) { result ->
            if (result[AppPermission.Notifications]?.isSatisfied == false) {
                AppMessageCenter.publish(getString(R.string.main_notification_permission_denied))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppContentTheme)
        super.onCreate(savedInstanceState)
        sensorController.initialize()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        permissionRequester.request(AppPermission.Notifications)

        setContent {
            MainScreen(
                activity = this,
                viewModel = viewModel,
                sensorController = sensorController,
                backupCoordinator = backupCoordinator
            )
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.handleIntent(MainIntent.UpdateInteraction)
    }

    override fun onResume() {
        super.onResume()
        if (sensorController.isFlipLockEnabled) sensorController.register()
    }

    override fun onPause() {
        super.onPause()
        if (sensorController.isFlipLockEnabled) sensorController.unregister()
    }

    private companion object {
        const val APP_CLOSE_MESSAGE_DELAY_MS = 300L
    }
}
