package com.aozijx.passly.presentation.feature.settings.main.navigation.autofill

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsEffect
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.autofill.toAutofillSettingsUiModel
import com.aozijx.passly.presentation.feature.settings.ui.autofill.AutofillDetail
import com.aozijx.passly.presentation.feature.settings.ui.main.component.SettingsGroup
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutofillRoute(
    onBack: (() -> Unit)?,
) {
    val viewModel: AutofillSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel, context) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AutofillSettingsEffect.OpenSystemAutofillSettings -> context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                        data = "package:${context.packageName}".toUri()
                    },
                )
            }
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.AUTOFILL.titleRes),
        onBack = onBack
    ) {
        item {
            AutofillDetail(
                state = state.toAutofillSettingsUiModel(
                    supportsCredentialManager =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                ),
                onAction = viewModel::onAction,
            )
        }
    }
}
