package com.aozijx.passly.feature.settings.presentation.main.navigation.autofill

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillPreferencesState
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsAction
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsEffect
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.ui.autofill.AutofillSettingsSection
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup

@Composable
internal fun AutofillRoute(onBack: (() -> Unit)?) {
    val viewModel: AutofillSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        onDispose { viewModel.onPageExited(owner) }
    }

    LaunchedEffect(viewModel, context, resources, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is AutofillSettingsEffect.SaveFailed -> {
                        Toast
                            .makeText(
                                context,
                                resources.getString(effect.message.resourceId),
                                Toast.LENGTH_SHORT,
                            ).show()
                    }

                    AutofillSettingsEffect.OpenSystemAutofillSettings -> {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                data = "package:${context.packageName}".toUri()
                            },
                        )
                    }

                    AutofillSettingsEffect.OpenAccessibilitySettings -> {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                        )
                    }
                }
            }
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.AUTOFILL.titleRes),
        onBack = onBack,
    ) {
        when (val preferences = state.preferences) {
            is AutofillPreferencesState.Unavailable -> {
                if (state.systemServiceStatus.canRetry) {
                    item {
                        AutofillSystemStatusFailure {
                            viewModel.onAction(AutofillSettingsAction.RetrySystemService)
                        }
                    }
                }
                item {
                    AutofillSettingsUnavailable(
                        statusTextRes = preferences.statusTextRes,
                        onRetry = { viewModel.onAction(AutofillSettingsAction.RetryPreferences) },
                        showRetry = preferences.canRetry,
                    )
                }
            }

            is AutofillPreferencesState.Ready -> {
                item {
                    SettingsSection {
                        AutofillSettingsSection(
                            settings = preferences.settings,
                            supportsCredentialManager = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                            systemServiceStatus = state.systemServiceStatus,
                            onAction = viewModel::onAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutofillSettingsUnavailable(
    statusTextRes: Int,
    showRetry: Boolean,
    onRetry: () -> Unit,
) {
    SettingsSection(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(stringResource(statusTextRes))
        if (showRetry) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun AutofillSystemStatusFailure(onRetry: () -> Unit) {
    SettingsSection(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(stringResource(R.string.settings_autofill_system_status_unavailable))
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
