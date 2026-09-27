package com.aozijx.passly.presentation.feature.settings.main.navigation.autofill

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsAction
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.autofill.toAutofillSettingsUiModel
import com.aozijx.passly.presentation.feature.settings.autofill.toDomainModel
import com.aozijx.passly.presentation.feature.settings.ui.autofill.AutofillDetail
import com.aozijx.passly.presentation.feature.settings.ui.autofill.model.AutofillSettingsEvent
import com.aozijx.passly.presentation.feature.settings.ui.main.component.SettingsGroup
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutofillRoute(
    onBack: (() -> Unit)?,
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
                onEvent = { event ->
                    val action = when (event) {
                        AutofillSettingsEvent.OpenSystemSettings ->
                            AutofillSettingsAction.OpenSystemAutofillSettings
                        is AutofillSettingsEvent.EnabledChanged ->
                            AutofillSettingsAction.SetEnabled(event.enabled)
                        is AutofillSettingsEvent.PresentationChanged ->
                            AutofillSettingsAction.SetPresentation(
                                event.presentation.toDomainModel(),
                            )
                        is AutofillSettingsEvent.CredentialManagerEnabledChanged ->
                            AutofillSettingsAction.SetCredentialManagerEnabled(event.enabled)
                        is AutofillSettingsEvent.AuthenticationRequiredChanged ->
                            AutofillSettingsAction.SetAuthenticationRequired(event.required)
                        is AutofillSettingsEvent.OtpEnabledChanged ->
                            AutofillSettingsAction.SetOtpEnabled(event.enabled)
                        is AutofillSettingsEvent.SavePromptsEnabledChanged ->
                            AutofillSettingsAction.SetSavePromptsEnabled(event.enabled)
                        is AutofillSettingsEvent.UnmatchedSuggestionsEnabledChanged ->
                            AutofillSettingsAction.SetUnmatchedSuggestionsEnabled(event.enabled)
                        is AutofillSettingsEvent.MaxSuggestionsChanged ->
                            AutofillSettingsAction.SetMaxSuggestions(event.maxSuggestions)
                    }
                    viewModel.onAction(action)
                },
            )
        }
    }
}
