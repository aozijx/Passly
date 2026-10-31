package com.aozijx.passly.presentation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.message.compose.ProvideAppNoticePublisher
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.presentation.AppNoticeEffects
import com.aozijx.passly.app.platform.permission.PermissionServices
import com.aozijx.passly.app.platform.permission.ProvidePermissionServices
import com.aozijx.passly.feature.shell.presentation.AppShell
import com.aozijx.passly.feature.shell.presentation.AppShellSettingsState
import com.aozijx.passly.feature.shell.presentation.AppShellViewModel
import com.aozijx.passly.feature.shell.presentation.AppWindowPolicyEffects
import com.aozijx.passly.feature.shell.presentation.windowPolicy
import com.aozijx.passly.feature.shell.presentation.theme.AppTheme
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry

@Composable
internal fun PasslyApp(
    activity: FragmentActivity,
    authenticationHostRegistry: AuthenticationHostRegistry,
    logger: Logger,
    noticePublisher: AppNoticePublisher,
    permissionServices: PermissionServices,
) {
    val shellViewModel = hiltViewModel<AppShellViewModel>()
    val shellState by shellViewModel.uiState.collectAsStateWithLifecycle()
    AppWindowPolicyEffects(activity.window, shellState.windowPolicy)

    val settings = shellState.settings as? AppShellSettingsState.Ready ?: return

    LaunchedEffect(settings.appearance.language) {
        val languageTags = settings.appearance.language.applicationLocaleTags
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != languageTags) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTags),
            )
        }
    }

    ProvidePermissionServices(permissionServices) {
        ProvideAppNoticePublisher(noticePublisher) {
            AppNoticeEffects()
            AppTheme(
                appearance = settings.appearance,
                appCornerRadiusDp = settings.appCornerRadiusDp,
            ) {
                AuthenticationHost(activity, authenticationHostRegistry, logger) {
                    AppShell(
                        activity = activity,
                        uiState = shellState,
                        effects = shellViewModel.effects,
                        onAction = shellViewModel::onAction,
                        onCloseApp = activity::finishAffinity,
                    )
                }
            }
        }
    }
}
