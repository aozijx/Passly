package com.aozijx.passly.presentation.feature.settings.main.navigation.general

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.presentation.feature.settings.main.general.GeneralDetail
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GeneralRoute(
    onBack: (() -> Unit)?,
) {
    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.GENERAL.titleRes),
        onBack = onBack
    ) {
        item { GeneralDetail() }
    }
}
