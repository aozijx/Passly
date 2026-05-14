package com.aozijx.passly

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.di.appViewModelFactory
import com.aozijx.passly.features.main.MainNotificationPermissionController
import com.aozijx.passly.features.main.MainSensorController
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.main.contract.MainIntent
import com.aozijx.passly.features.main.ui.MainScreen

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels { appViewModelFactory(application) }

    private val sensorController: MainSensorController by lazy {
        MainSensorController(this) {
            if (viewModel.isAuthorizedNow()) {
                viewModel.handleIntent(MainIntent.Lock)
                if (sensorController.isFlipExitAndClearStackEnabled) finishAndRemoveTask()
            }
        }
    }

    private val notificationPermissionController: MainNotificationPermissionController by lazy {
        MainNotificationPermissionController(this) {
            Toast.makeText(
                this,
                getString(R.string.main_notification_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorController.initialize()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        notificationPermissionController.requestIfNeeded()

        setContent {
            MainScreen(
                activity = this,
                viewModel = viewModel,
                sensorController = sensorController
            )
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.handleIntent(MainIntent.UpdateInteraction)
    }

    override fun onResume() {
        super.onResume()
        viewModel.handleIntent(MainIntent.CheckAndLock)
        if (sensorController.isFlipLockEnabled) sensorController.register()
    }

    override fun onPause() {
        super.onPause()
        if (sensorController.isFlipLockEnabled) sensorController.unregister()
    }
}