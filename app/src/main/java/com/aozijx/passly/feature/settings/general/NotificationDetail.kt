package com.aozijx.passly.feature.settings.general

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.provider.Settings
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.core.permission.compose.rememberPermissionRequestHost
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.domain.notice.model.NoticeCode
import com.aozijx.passly.domain.notice.model.newAppNotice

@Composable
internal fun NotificationDetail(
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noticePublisher = LocalAppNoticePublisher.current
    val permissionHost = rememberPermissionRequestHost("settings.notifications") { permission, result ->
        if (
            permission == RuntimePermission.POST_NOTIFICATIONS &&
            result is PermissionRequestOutcome.Denied
        ) {
            noticePublisher.publish(newAppNotice(NoticeCode.NOTIFICATION_PERMISSION_DENIED))
        }
    }

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))
        NotificationSettingsSection(
            state = state,
            onSystemNotificationsEnabledChange = { enabled ->
                viewModel.setSystemNotificationsEnabled(enabled)
                if (
                    enabled &&
                    permissionHost.status(RuntimePermission.POST_NOTIFICATIONS) !=
                    PermissionStatus.GRANTED
                ) {
                    permissionHost.request(RuntimePermission.POST_NOTIFICATIONS)
                }
            },
            onOpenSystemNotificationSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                )
            },
            onOptionalMessagesEnabledChange = viewModel::setOptionalMessagesEnabled,
            onTopicEnabledChange = viewModel::setMessageTopicEnabled
        )
    }
}
