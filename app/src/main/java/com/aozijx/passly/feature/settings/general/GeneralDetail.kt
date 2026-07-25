package com.aozijx.passly.feature.settings.general

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.R
import com.aozijx.passly.core.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.core.permission.compose.rememberPermissionRequestHost
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.platform.CacheUtils
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.domain.notice.model.NoticeCode
import com.aozijx.passly.domain.notice.model.newAppNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GeneralDetail() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf(CacheUtils.calculateTotalCacheSize(context)) }
    val notificationViewModel: NotificationSettingsViewModel = hiltViewModel()
    val notificationState by notificationViewModel.uiState.collectAsStateWithLifecycle()
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
            state = notificationState,
            onSystemNotificationsEnabledChange = { enabled ->
                notificationViewModel.setSystemNotificationsEnabled(enabled)
                when {
                    !enabled -> Unit
                    permissionHost.status(RuntimePermission.POST_NOTIFICATIONS) ==
                        PermissionStatus.GRANTED -> Unit
                    else -> permissionHost.request(RuntimePermission.POST_NOTIFICATIONS)
                }
            },
            onOptionalMessagesEnabledChange = notificationViewModel::setOptionalMessagesEnabled,
            onTopicEnabledChange = notificationViewModel::setMessageTopicEnabled
        )

        Spacer(modifier = Modifier.height(24.dp))

        CacheSettingsSection(
            cacheSize = cacheSize,
            onClearCache = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        CacheUtils.clearAllCache(context)
                    }
                    cacheSize = CacheUtils.calculateTotalCacheSize(context)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        LogSettingsSection()

        Spacer(modifier = Modifier.height(24.dp))

        AboutSettingsSection(
            appVersion = BuildConfig.VERSION_NAME,
            onAboutClick = {}
        )
    }
}
