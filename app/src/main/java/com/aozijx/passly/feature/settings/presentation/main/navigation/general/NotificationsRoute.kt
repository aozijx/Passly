package com.aozijx.passly.feature.settings.presentation.main.navigation.general

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.platform.permission.rememberPermissionRequestHost
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.feature.settings.presentation.main.general.NotificationSettingsAction
import com.aozijx.passly.feature.settings.presentation.main.general.NotificationPermissionRequest
import com.aozijx.passly.feature.settings.presentation.main.general.NotificationSettingsEffect
import com.aozijx.passly.feature.settings.presentation.main.general.NotificationSettingsUiState
import com.aozijx.passly.feature.settings.presentation.main.general.NotificationSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.main.general.toUiModel
import com.aozijx.passly.feature.settings.presentation.ui.general.NotificationSettingsSection
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup

private sealed interface NotificationPermissionLaunch {
    data object Idle : NotificationPermissionLaunch
    data class Pending(val request: NotificationPermissionRequest) : NotificationPermissionLaunch
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsRoute(
    onBack: (() -> Unit)?,
) {
    val viewModel: NotificationSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val noticePublisher = LocalAppNoticePublisher.current
    var permissionLaunch by remember { mutableStateOf<NotificationPermissionLaunch>(NotificationPermissionLaunch.Idle) }

    fun publishPermissionDeniedNotice() {
        noticePublisher.publish(newAppNotice(NoticeCode.NOTIFICATION_PERMISSION_DENIED))
    }

    val permissionHost = rememberPermissionRequestHost("settings.notifications") { permission, result ->
        if (permission != RuntimePermission.POST_NOTIFICATIONS) {
            return@rememberPermissionRequestHost
        }
        val pending = permissionLaunch as? NotificationPermissionLaunch.Pending ?: return@rememberPermissionRequestHost
        permissionLaunch = NotificationPermissionLaunch.Idle
        when (result) {
            PermissionRequestOutcome.Granted ->
                viewModel.onAction(NotificationSettingsAction.RuntimePermissionGranted(pending.request))

            is PermissionRequestOutcome.Denied ->
                viewModel.onAction(NotificationSettingsAction.RuntimePermissionDenied(pending.request))
        }
    }

    LaunchedEffect(viewModel, permissionHost, context, noticePublisher, resources, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is NotificationSettingsEffect.SaveFailed -> Toast.makeText(
                        context, resources.getString(effect.message.resourceId), Toast.LENGTH_SHORT,
                    ).show()
                    is NotificationSettingsEffect.RequestRuntimeNotificationPermission -> {
                        if (!viewModel.canLaunchRuntimePermissionRequest(effect.request)) {
                            viewModel.onAction(NotificationSettingsAction.RuntimePermissionDenied(effect.request))
                            return@collect
                        }
                        permissionLaunch = NotificationPermissionLaunch.Pending(effect.request)
                        when (permissionHost.request(RuntimePermission.POST_NOTIFICATIONS)) {
                            PermissionRequestStart.Launched -> Unit
                            PermissionRequestStart.AlreadyGranted,
                            PermissionRequestStart.NotApplicable -> {
                                permissionLaunch = NotificationPermissionLaunch.Idle
                                viewModel.onAction(NotificationSettingsAction.RuntimePermissionGranted(effect.request))
                            }

                            PermissionRequestStart.Busy,
                            PermissionRequestStart.HostUnavailable -> {
                                permissionLaunch = NotificationPermissionLaunch.Idle
                                viewModel.onAction(NotificationSettingsAction.RuntimePermissionDenied(effect.request))
                            }
                        }
                    }

                    NotificationSettingsEffect.OpenSystemNotificationSettings -> {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            },
                        )
                    }

                    NotificationSettingsEffect.ShowPermissionDenied ->
                        publishPermissionDeniedNotice()
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val owner = viewModel.onPageEntered()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(NotificationSettingsAction.RefreshSystemNotificationState)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            permissionLaunch = NotificationPermissionLaunch.Idle
            viewModel.onAction(NotificationSettingsAction.CancelRuntimePermissionRequest)
            viewModel.onPageExited(owner)
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.NOTIFICATIONS.titleRes),
        onBack = onBack,
    ) {
        when (val readyState = state) {
            NotificationSettingsUiState.Loading -> Unit
            is NotificationSettingsUiState.Ready -> item {
                SettingsSection {
                    NotificationSettingsSection(
                        state = readyState.toUiModel(),
                        onAction = viewModel::onAction,
                    )
                }
            }
        }
    }
}
