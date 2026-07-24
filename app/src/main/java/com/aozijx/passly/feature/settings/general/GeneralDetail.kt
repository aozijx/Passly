package com.aozijx.passly.feature.settings.general

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.core.permission.AppPermission
import com.aozijx.passly.core.permission.rememberAppPermissionRequester
import com.aozijx.passly.core.platform.CacheUtils
import com.aozijx.passly.core.ui.components.settings.SettingsSection
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
    val permissionDeniedMessage = stringResource(R.string.main_notification_permission_denied)
    val permissionRequester = rememberAppPermissionRequester { result ->
        val granted = result[AppPermission.Notifications]?.isSatisfied == true
        notificationViewModel.setStatusBarEnabled(granted)
        if (!granted) AppMessageCenter.publish(permissionDeniedMessage)
    }

    LaunchedEffect(notificationState.statusBarEnabled, permissionRequester) {
        if (
            notificationState.statusBarEnabled &&
            !permissionRequester.snapshot(AppPermission.Notifications).isSatisfied
        ) {
            notificationViewModel.setStatusBarEnabled(false)
        }
    }

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        NotificationSettingsSection(
            state = notificationState,
            onStatusBarEnabledChange = { enabled ->
                when {
                    !enabled -> notificationViewModel.setStatusBarEnabled(false)
                    permissionRequester.snapshot(AppPermission.Notifications).isSatisfied ->
                        notificationViewModel.setStatusBarEnabled(true)

                    else -> permissionRequester.request(AppPermission.Notifications)
                }
            },
            onIconDownloadsEnabledChange = notificationViewModel::setIconDownloadsEnabled
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
