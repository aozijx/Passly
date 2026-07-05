package com.aozijx.passly

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.platform.LocaleHelper
import com.aozijx.passly.data.repository.settings.internal.settingsDataStore
import com.aozijx.passly.ui.features.backup.BackupCoordinator
import com.aozijx.passly.ui.features.main.MainNotificationPermissionController
import com.aozijx.passly.ui.features.main.MainSensorController
import com.aozijx.passly.ui.features.main.MainViewModel
import com.aozijx.passly.ui.features.main.contract.MainIntent
import com.aozijx.passly.ui.features.main.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var backupCoordinator: BackupCoordinator

    private val sensorController: MainSensorController by lazy {
        MainSensorController(this) {
            if (viewModel.isAuthorizedNow()) {
                viewModel.handleIntent(MainIntent.Lock)
                if (sensorController.isFlipExitAndClearStackEnabled) {
                    finishAndRemoveTask()
                    exitProcess(0)
                }
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

    override fun attachBaseContext(newBase: Context) {
        val code = runCatching {
            kotlinx.coroutines.runBlocking {
                newBase.settingsDataStore.data.first()[
                    stringPreferencesKey("app_language_code")
                ] ?: ""
            }
        }.getOrDefault("")
        val ctx = if (code.isNotEmpty()) {
            LocaleHelper.applyLanguage(newBase, code)
        } else {
            newBase
        }
        super.attachBaseContext(ctx)
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

        notificationPermissionController.requestIfNeeded()

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
        viewModel.handleIntent(MainIntent.CheckAndLock)
        if (sensorController.isFlipLockEnabled) sensorController.register()
    }

    override fun onPause() {
        super.onPause()
        if (sensorController.isFlipLockEnabled) sensorController.unregister()
    }
}