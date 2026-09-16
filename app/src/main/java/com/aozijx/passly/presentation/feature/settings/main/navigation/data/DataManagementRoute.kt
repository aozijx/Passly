package com.aozijx.passly.presentation.feature.settings.main.navigation.data

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.presentation.ui.settings.backup.DataManagementDetail
import com.aozijx.passly.presentation.ui.settings.backup.model.DataManagementEventHandler
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataManagementRoute(
    localState: SettingsScreenLocalState,
    onOpenTrash: () -> Unit,
    onBack: (() -> Unit)?,
) {
    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.DATA_MANAGEMENT.titleRes),
        onBack = onBack
    ) {
        item {
            DataManagementDetail(
                eventHandler = object : DataManagementEventHandler {
                    override fun onOpenTrash() = onOpenTrash.invoke()
                    override fun onOpenDatabaseReset() = localState.openDatabaseResetSheet()
                },
            )
        }
    }
}
