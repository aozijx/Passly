package com.aozijx.passly.feature.settings.general

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.platform.CacheUtils
import com.aozijx.passly.feature.settings.shell.sectionSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GeneralDetail() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf(CacheUtils.calculateTotalCacheSize(context)) }
    val messageViewModel: MessageSettingsViewModel = hiltViewModel()
    val messageState by messageViewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        MessageSettingsSection(
            state = messageState,
            onShowGeneralChange = messageViewModel::setShowGeneral,
            onShowIconDownloadsChange = messageViewModel::setShowIconDownloads,
            onShowClipboardClearsChange = messageViewModel::setShowClipboardClears,
            onShowAppCloseChange = messageViewModel::setShowAppClose
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
