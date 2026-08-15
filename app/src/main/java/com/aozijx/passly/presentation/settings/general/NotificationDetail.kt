package com.aozijx.passly.presentation.settings.general

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.core.permission.compose.rememberPermissionRequestHost
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.feature.settings.general.NotificationSettingsViewModel
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice

@Composable
internal fun NotificationDetail(
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val noticePublisher = LocalAppNoticePublisher.current
    fun publishPermissionDeniedNotice() {
        noticePublisher.publish(newAppNotice(NoticeCode.NOTIFICATION_PERMISSION_DENIED))
    }

    fun openSystemNotificationSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        )
    }

    val permissionHost = rememberPermissionRequestHost("settings.notifications") { permission, result ->
        if (permission != RuntimePermission.POST_NOTIFICATIONS) return@rememberPermissionRequestHost
        when (result) {
            PermissionRequestOutcome.Granted -> {
                if (viewModel.systemNotificationsAvailableNow()) {
                    viewModel.setSystemNotificationsEnabled(true)
                } else {
                    publishPermissionDeniedNotice()
                    openSystemNotificationSettings()
                }
            }

            is PermissionRequestOutcome.Denied -> publishPermissionDeniedNotice()
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemNotificationState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))
        NotificationSettingsSection(
            state = state,
            onSystemNotificationsEnabledChange = { enabled ->
                if (!enabled) {
                    viewModel.setSystemNotificationsEnabled(false)
                    return@NotificationSettingsSection
                }
                when (permissionHost.status(RuntimePermission.POST_NOTIFICATIONS)) {
                    PermissionStatus.GRANTED,
                    PermissionStatus.NOT_APPLICABLE -> {
                        if (viewModel.systemNotificationsAvailableNow()) {
                            viewModel.setSystemNotificationsEnabled(true)
                        } else {
                            publishPermissionDeniedNotice()
                            openSystemNotificationSettings()
                        }
                    }

                    PermissionStatus.DENIED -> {
                        when (permissionHost.request(RuntimePermission.POST_NOTIFICATIONS)) {
                            PermissionRequestStart.Launched -> Unit
                            PermissionRequestStart.AlreadyGranted,
                            PermissionRequestStart.NotApplicable -> {
                                if (viewModel.systemNotificationsAvailableNow()) {
                                    viewModel.setSystemNotificationsEnabled(true)
                                } else {
                                    publishPermissionDeniedNotice()
                                    openSystemNotificationSettings()
                                }
                            }

                            PermissionRequestStart.Busy,
                            PermissionRequestStart.HostUnavailable -> publishPermissionDeniedNotice()
                        }
                    }
                }
            },
            onOpenSystemNotificationSettings = ::openSystemNotificationSettings,
            onOptionalMessagesEnabledChange = viewModel::setOptionalMessagesEnabled,
            onTopicEnabledChange = viewModel::setMessageTopicEnabled
        )
    }
}
