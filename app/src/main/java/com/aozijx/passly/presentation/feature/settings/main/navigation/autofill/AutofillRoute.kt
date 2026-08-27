package com.aozijx.passly.presentation.feature.settings.main.navigation.autofill

import com.aozijx.passly.presentation.feature.settings.main.navigation.SettingsRoute
import android.content.Context
import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsAction
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.autofill.toAutofillSettingsUiModel
import com.aozijx.passly.presentation.feature.settings.autofill.toDomainModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.ui.settings.autofill.AutofillDetail
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutofillRouteContent(
    route: SettingsRoute,
    context: Context,
    localState: SettingsScreenLocalState,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState,
    onBack: (() -> Unit)?
) {
    val viewModel: AutofillSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                onOpenAutofillSettings = {
                    viewModel.onAction(
                        AutofillSettingsAction.OpenSystemAutofillSettings
                    )
                },
                onEnabledChange = {
                    viewModel.onAction(AutofillSettingsAction.SetEnabled(it))
                },
                onPresentationChange = {
                    viewModel.onAction(
                        AutofillSettingsAction.SetPresentation(it.toDomainModel())
                    )
                },
                onCredentialManagerEnabledChange = {
                    viewModel.onAction(
                        AutofillSettingsAction.SetCredentialManagerEnabled(it)
                    )
                },
                onAuthenticationRequiredChange = {
                    viewModel.onAction(
                        AutofillSettingsAction.SetAuthenticationRequired(it)
                    )
                },
                onOtpEnabledChange = {
                    viewModel.onAction(AutofillSettingsAction.SetOtpEnabled(it))
                },
                onSavePromptsEnabledChange = {
                    viewModel.onAction(
                        AutofillSettingsAction.SetSavePromptsEnabled(it)
                    )
                },
                onUnmatchedSuggestionsEnabledChange = {
                    viewModel.onAction(
                        AutofillSettingsAction.SetUnmatchedSuggestionsEnabled(it)
                    )
                },
                onMaxSuggestionsChange = {
                    viewModel.onAction(AutofillSettingsAction.SetMaxSuggestions(it))
                },
            )
        }
    }
}
