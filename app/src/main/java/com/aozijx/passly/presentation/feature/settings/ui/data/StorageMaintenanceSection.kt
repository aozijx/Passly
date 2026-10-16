package com.aozijx.passly.presentation.feature.settings.ui.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.feature.settings.main.data.StorageSizeUiState

@Composable
internal fun CacheSettingsSection(
    state: StorageSizeUiState,
    onClearCache: () -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_storage_cache_section))
    SegmentedSettingsGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "storage.clear_cache",
                icon = Icons.Default.DeleteSweep,
                title = stringResource(R.string.settings_storage_clear_cache),
                value = (state as? StorageSizeUiState.Available)?.formattedSize,
                isLoading = state is StorageSizeUiState.Loading,
                onClick = onClearCache
            )
        )
    )
}
