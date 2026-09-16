package com.aozijx.passly.presentation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.message.compose.ProvideAppNoticePublisher
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.platform.permission.PermissionServices
import com.aozijx.passly.app.platform.permission.ProvidePermissionServices
import com.aozijx.passly.app.shell.FlipToLockSensorController
import com.aozijx.passly.presentation.feature.shell.AppShell
import com.aozijx.passly.presentation.feature.shell.AppShellUiAction
import com.aozijx.passly.presentation.feature.shell.AppShellViewModel
import com.aozijx.passly.presentation.feature.shell.theme.AppTheme
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry

@Composable
internal fun PasslyApp(
    activity: FragmentActivity,
    shellViewModel: AppShellViewModel,
    sensorController: FlipToLockSensorController,
    authenticationHostRegistry: AuthenticationHostRegistry,
    noticePublisher: AppNoticePublisher,
    permissionServices: PermissionServices,
) {
    val shellState by shellViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(shellState.appearance.language) {
        val languageTags = shellState.appearance.language.applicationLocaleTags
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != languageTags) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTags),
            )
        }
    }

    ProvidePermissionServices(permissionServices) {
        ProvideAppNoticePublisher(noticePublisher) {
            AppTheme(
                appearance = shellState.appearance,
                appCornerRadiusDp = shellState.appCornerRadiusDp,
            ) {
                AuthenticationHost(activity, authenticationHostRegistry) {
                    AppShell(
                        window = activity.window,
                        uiState = shellState,
                        effects = shellViewModel.effects,
                        onAction = shellViewModel::onAction,
                        onCloseApp = activity::finishAffinity,
                        sensorController = sensorController,
                    )
                }
            }
        }
    }
}
