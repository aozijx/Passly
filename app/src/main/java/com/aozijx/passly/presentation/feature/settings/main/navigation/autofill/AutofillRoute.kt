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
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillPresentationUiModel
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsEventHandler
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
                eventHandler = object : AutofillSettingsEventHandler {
                    override fun onOpenSystemSettings() = viewModel.onAction(
                        AutofillSettingsAction.OpenSystemAutofillSettings,
                    )
                    override fun onEnabledChanged(enabled: Boolean) =
                        viewModel.onAction(AutofillSettingsAction.SetEnabled(enabled))
                    override fun onPresentationChanged(presentation: AutofillPresentationUiModel) =
                        viewModel.onAction(
                            AutofillSettingsAction.SetPresentation(presentation.toDomainModel()),
                        )
                    override fun onCredentialManagerEnabledChanged(enabled: Boolean) =
                        viewModel.onAction(
                            AutofillSettingsAction.SetCredentialManagerEnabled(enabled),
                        )
                    override fun onAuthenticationRequiredChanged(required: Boolean) =
                        viewModel.onAction(
                            AutofillSettingsAction.SetAuthenticationRequired(required),
                        )
                    override fun onOtpEnabledChanged(enabled: Boolean) =
                        viewModel.onAction(AutofillSettingsAction.SetOtpEnabled(enabled))
                    override fun onSavePromptsEnabledChanged(enabled: Boolean) =
                        viewModel.onAction(
                            AutofillSettingsAction.SetSavePromptsEnabled(enabled),
                        )
                    override fun onUnmatchedSuggestionsEnabledChanged(enabled: Boolean) =
                        viewModel.onAction(
                            AutofillSettingsAction.SetUnmatchedSuggestionsEnabled(enabled),
                        )
                    override fun onMaxSuggestionsChanged(maxSuggestions: Int) =
                        viewModel.onAction(
                            AutofillSettingsAction.SetMaxSuggestions(maxSuggestions),
                        )
                },
            )
        }
    }
}
