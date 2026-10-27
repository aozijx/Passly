package com.aozijx.passly.feature.settings.presentation.main.navigation.autofill

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsEffect
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsUiState
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.autofill.toAutofillSettingsUiModel
import com.aozijx.passly.feature.settings.presentation.ui.autofill.AutofillDetail
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutofillRoute(
    onBack: (() -> Unit)?,
) {
    val viewModel: AutofillSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, context, resources, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is AutofillSettingsEffect.SaveFailed -> Toast.makeText(
                        context, resources.getString(effect.message.resourceId), Toast.LENGTH_SHORT,
                    ).show()
                    AutofillSettingsEffect.OpenSystemAutofillSettings -> context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                            data = "package:${context.packageName}".toUri()
                        },
                    )
                    AutofillSettingsEffect.OpenAccessibilitySettings -> context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                    )
                }
            }
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.AUTOFILL.titleRes),
        onBack = onBack
    ) {
        when (val readyState = state) {
            AutofillSettingsUiState.Loading -> Unit
            is AutofillSettingsUiState.Ready -> item {
                AutofillDetail(
                    state = readyState.toAutofillSettingsUiModel(
                        supportsCredentialManager =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                    ),
                    onAction = viewModel::onAction,
                )
            }
        }
    }
}
