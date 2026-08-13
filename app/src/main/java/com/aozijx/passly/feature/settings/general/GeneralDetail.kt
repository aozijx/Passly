package com.aozijx.passly.feature.settings.general

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.platform.CacheUtils
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GeneralDetail() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf<String?>(null) }
    var isCalculating by remember { mutableStateOf(true) }
    val diagnosticsViewModel: DiagnosticsSettingsViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        cacheSize = withContext(Dispatchers.IO) {
            CacheUtils.calculateTotalCacheSize(context)
        }
        isCalculating = false
    }
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        CacheSettingsSection(
            cacheSize = cacheSize,
            isLoading = isCalculating,
            onClearCache = {
                scope.launch {
                    isCalculating = true
                    cacheSize = withContext(Dispatchers.IO) {
                        CacheUtils.clearAllCache(context)
                        CacheUtils.calculateTotalCacheSize(context)
                    }
                    isCalculating = false
                }
            }
        )

        Spacer(Modifier.height(24.dp))

        LogSettingsSection(viewModel = diagnosticsViewModel)

        Spacer(Modifier.height(24.dp))

        AboutSettingsSection(
            appVersion = BuildConfig.VERSION_NAME,
            onAppDetailsClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onAboutClick = {}
        )
    }
}
