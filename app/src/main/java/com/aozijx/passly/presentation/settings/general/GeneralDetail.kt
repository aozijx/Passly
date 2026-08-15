package com.aozijx.passly.presentation.settings.general

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.feature.settings.general.CacheSettingsViewModel
import com.aozijx.passly.feature.settings.general.DiagnosticsSettingsViewModel

@Composable
internal fun GeneralDetail() {
    val context = LocalContext.current
    val cacheViewModel: CacheSettingsViewModel = hiltViewModel()
    val cacheSize by cacheViewModel.cacheSize.collectAsStateWithLifecycle()
    val isCalculating by cacheViewModel.isCalculating.collectAsStateWithLifecycle()
    val diagnosticsViewModel: DiagnosticsSettingsViewModel = hiltViewModel()

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        CacheSettingsSection(
            cacheSize = cacheSize,
            isLoading = isCalculating,
            onClearCache = cacheViewModel::clearCache
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
