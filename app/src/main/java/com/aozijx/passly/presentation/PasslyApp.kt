package com.aozijx.passly.presentation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.app.message.compose.ProvideAppNoticePublisher
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.presentation.AppNoticeEffects
import com.aozijx.passly.app.platform.permission.PermissionServices
import com.aozijx.passly.app.platform.permission.ProvidePermissionServices
import com.aozijx.passly.app.theme.AppTheme
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeRoute
import com.aozijx.passly.feature.shell.presentation.AppShell
import com.aozijx.passly.feature.shell.presentation.AppShellUiAction
import com.aozijx.passly.feature.shell.presentation.AppShellViewModel
import com.aozijx.passly.feature.shell.presentation.AppWindowPolicyEffects
import com.aozijx.passly.feature.shell.presentation.windowPolicy
import com.aozijx.passly.feature.unlock.presentation.AuthenticationRoute
import com.aozijx.passly.presentation.shared.state.SettingsReadFailureContent
import com.aozijx.passly.security.authentication.host.AuthenticationHost

@Composable
internal fun PasslyApp(
    activity: FragmentActivity,
    noticePublisher: AppNoticePublisher,
    permissionServices: PermissionServices,
) {
    val shellViewModel = hiltViewModel<AppShellViewModel>()
    val shellState by shellViewModel.uiState.collectAsStateWithLifecycle()
    AppWindowPolicyEffects(activity.window, shellState.windowPolicy)

    val settings =
        when (val state = shellState.settings) {
            RecoverableReadState.Loading -> {
                return
            }

            is RecoverableReadState.Failed -> {
                AppTheme(
                    appearance =
                        AppearanceSettings(
                            themeMode = ThemeMode.SYSTEM,
                            isDynamicColor = true,
                            themeKey = "default",
                            language = AppLanguage.SYSTEM,
                            fontFamily = FontFamilyMode.APP_BUNDLED,
                        ),
                    appCornerRadiusDp = AppCornerRadiusConstraints.DEFAULT_DP,
                ) {
                    SettingsReadFailureContent(
                        onRetry = { shellViewModel.onAction(AppShellUiAction.RetrySettings) },
                    )
                }
                return
            }

            is RecoverableReadState.Ready -> {
                state.value
            }
        }

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
                AuthenticationHost(activity) {
                    AppShell(
                        uiState = shellState,
                        effects = shellViewModel.effects,
                        onAction = shellViewModel::onAction,
                        onCloseApp = activity::finishAffinity,
                        vaultContent = { PasslyAppNavigation(activity) },
                        recoveryContent = { onExit -> RecoveryModeRoute(onExit = onExit) },
                        authenticationContent = { AuthenticationRoute() },
                    )
                }
            }
        }
    }
}
