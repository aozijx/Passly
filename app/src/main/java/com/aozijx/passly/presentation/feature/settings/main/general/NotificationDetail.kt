package com.aozijx.passly.presentation.feature.settings.main.general

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.app.platform.permission.rememberPermissionRequestHost
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.feature.settings.main.general.NotificationSettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.general.NotificationSettingsViewModel
import com.aozijx.passly.presentation.ui.settings.general.NotificationSettingsSection
import com.aozijx.passly.presentation.ui.settings.general.NotificationSettingsEventHandler
import com.aozijx.passly.presentation.ui.settings.general.NotificationTopic

@Composable
internal fun NotificationDetail(
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val noticePublisher = LocalAppNoticePublisher.current

    // 导航副作用由 ViewModel 以 Effect 发出，UI 只负责执行（MVI）。
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NotificationSettingsEffect.OpenSystemNotificationSettings -> {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    )
                }
            }
        }
    }

    fun publishPermissionDeniedNotice() {
        noticePublisher.publish(newAppNotice(NoticeCode.NOTIFICATION_PERMISSION_DENIED))
    }

    val permissionHost = rememberPermissionRequestHost("settings.notifications") { permission, result ->
        if (permission != RuntimePermission.POST_NOTIFICATIONS) return@rememberPermissionRequestHost
        when (result) {
            PermissionRequestOutcome.Granted -> {
                if (viewModel.systemNotificationsAvailableNow()) {
                    viewModel.setSystemNotificationsEnabled(true)
                } else {
                    publishPermissionDeniedNotice()
                    viewModel.openSystemNotificationSettings()
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
            state = state.toUiModel(),
            eventHandler = object : NotificationSettingsEventHandler {
                override fun onSystemNotificationsEnabledChanged(enabled: Boolean) {
                    if (!enabled) {
                        viewModel.setSystemNotificationsEnabled(false)
                        return
                    }
                    when (permissionHost.status(RuntimePermission.POST_NOTIFICATIONS)) {
                        PermissionStatus.GRANTED,
                        PermissionStatus.NOT_APPLICABLE -> {
                            if (viewModel.systemNotificationsAvailableNow()) {
                                viewModel.setSystemNotificationsEnabled(true)
                            } else {
                                publishPermissionDeniedNotice()
                                viewModel.openSystemNotificationSettings()
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
                                        viewModel.openSystemNotificationSettings()
                                    }
                                }

                                PermissionRequestStart.Busy,
                                PermissionRequestStart.HostUnavailable ->
                                    publishPermissionDeniedNotice()
                            }
                        }
                    }
                }

                override fun onOpenSystemNotificationSettings() =
                    viewModel.openSystemNotificationSettings()
                override fun onOptionalMessagesEnabledChanged(enabled: Boolean) {
                    viewModel.setOptionalMessagesEnabled(enabled)
                }

                override fun onTopicEnabledChanged(topic: NotificationTopic, enabled: Boolean) {
                    viewModel.setMessageTopicEnabled(topic.toFeatureModel(), enabled)
                }
            },
        )
    }
}
