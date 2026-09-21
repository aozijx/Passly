package com.aozijx.passly.presentation.feature.settings.main.navigation.data

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.presentation.feature.database.reset.DatabaseResetOverlay
import com.aozijx.passly.presentation.feature.settings.ui.data.DataManagementDetail
import com.aozijx.passly.presentation.feature.settings.ui.data.model.DataManagementEventHandler
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataManagementRoute(
    onOpenTrash: () -> Unit,
    onBack: (() -> Unit)?,
) {
    var showDatabaseReset by rememberSaveable { mutableStateOf(false) }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.DATA_MANAGEMENT.titleRes),
        onBack = onBack,
    ) {
        item {
            DataManagementDetail(
                eventHandler = object : DataManagementEventHandler {
                    override fun onOpenTrash() = onOpenTrash()
                    override fun onOpenDatabaseReset() {
                        showDatabaseReset = true
                    }
                },
            )
        }
    }

    if (showDatabaseReset) {
        DatabaseResetOverlay(onDismiss = { showDatabaseReset = false })
    }
}
