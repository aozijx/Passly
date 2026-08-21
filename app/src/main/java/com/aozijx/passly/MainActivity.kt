package com.aozijx.passly

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.message.compose.ProvideAppNoticePublisher
import com.aozijx.passly.core.permission.compose.PermissionServices
import com.aozijx.passly.core.permission.compose.ProvidePermissionServices
import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.request.PermissionRequestArbiter
import com.aozijx.passly.core.ui.components.auth.AuthenticationHost
import com.aozijx.passly.core.ui.theme.AppTheme
import com.aozijx.passly.app.shell.AppShellViewModel
import com.aozijx.passly.app.shell.FlipToLockSensorController
import com.aozijx.passly.app.shell.contract.AppShellIntent
import com.aozijx.passly.app.shell.ui.AppShell
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess

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

    private val sensorController: FlipToLockSensorController by lazy {
        FlipToLockSensorController(this) {
            if (viewModel.isAuthorizedNow) {
                viewModel.handleIntent(AppShellIntent.Lock)
                if (sensorController.isFlipExitAndClearStackEnabled) {
                    noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER))
                    window.decorView.postDelayed({
                        finishAndRemoveTask()
                        exitProcess(0)
                    }, APP_CLOSE_MESSAGE_DELAY_MS)
                }
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

        setContent {
            val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()

            // 响应语言切换
            LaunchedEffect(mainUiState.language) {
                val tag = mainUiState.language.applicationLocaleTags
                val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                if (currentTags != tag) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                }
            }

            ProvidePermissionServices(
                PermissionServices(
                    statusReader = permissionStatusReader,
                    requestArbiter = permissionRequestArbiter,
                    requestHistory = permissionRequestHistory
                )
            ) {
                ProvideAppNoticePublisher(noticePublisher) {
                    AppTheme(
                        themeMode = mainUiState.themeMode,
                        dynamicColor = mainUiState.isDynamicColor,
                        fallbackPalette = mainUiState.fallbackPalette,
                        fontFamily = mainUiState.fontFamily,
                        outerCornerRadiusDp = mainUiState.outerCornerRadiusDp,
                        innerCornerRadiusDp = mainUiState.innerCornerRadiusDp,
                        groupItemSpacingDp = mainUiState.groupItemSpacingDp,
                        groupContentPaddingDp = mainUiState.groupContentPaddingDp
                    ) {
                        AuthenticationHost(this, authenticationHostRegistry) {
                            AppShell(
                                activity = this,
                                viewModel = viewModel,
                                sensorController = sensorController
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.handleIntent(AppShellIntent.UpdateInteraction)
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
        const val APP_CLOSE_MESSAGE_DELAY_MS = 1_000L
    }
}
