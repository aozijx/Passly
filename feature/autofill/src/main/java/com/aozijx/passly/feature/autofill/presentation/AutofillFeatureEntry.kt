package com.aozijx.passly.feature.autofill.presentation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.platform.AutofillHostLaunchContract
import com.aozijx.passly.feature.autofill.platform.AutofillIntentExtras
import com.aozijx.passly.feature.autofill.presentation.accessibility.AccessibilityAutofillEntry
import com.aozijx.passly.feature.autofill.presentation.credential.CredentialAutofillEntry
import com.aozijx.passly.feature.autofill.presentation.credential.CredentialResponseUiAction
import com.aozijx.passly.feature.autofill.presentation.legacy.AutofillFillRequest
import com.aozijx.passly.feature.autofill.presentation.legacy.LegacyAutofillEntry
import com.aozijx.passly.feature.autofill.presentation.legacy.toAutofillFillRequest
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent

/** Minimal platform boundary returned to the App-owned Activity. */
data class AutofillHostCompletion(
    val resultCode: Int? = null,
    val data: Intent? = null,
    val hideIme: Boolean = true,
)

/** Parses one launch and dispatches it to the matching Autofill workflow. */
@Composable
fun AutofillFeatureEntry(
    sourceIntent: Intent,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    val activity =
        LocalActivity.current as? ComponentActivity
            ?: error("AutofillFeatureEntry requires a ComponentActivity host")
    val dependencies = EntryPointAccessors.fromActivity(activity, AutofillEntryPoint::class.java)
    val launch = remember(sourceIntent) { parseLaunch(sourceIntent) }

    when (val current = launch) {
        null -> {
            LaunchedEffect(sourceIntent) {
                onComplete(AutofillHostCompletion(resultCode = android.app.Activity.RESULT_CANCELED))
            }
        }

        is AutofillLaunch.Legacy -> {
            LegacyAutofillEntry(current, activity, dependencies, onComplete)
        }

        AutofillLaunch.Accessibility -> {
            AccessibilityAutofillEntry(onComplete)
        }

        is AutofillLaunch.Credential -> {
            CredentialAutofillEntry(current, dependencies, onComplete)
        }
    }
}

@EntryPoint
@InstallIn(ActivityComponent::class)
internal interface AutofillEntryPoint {
    fun logger(): Logger

    fun responseFactory(): LegacyResponseFactory

    fun resultDelivery(): AutofillResultDelivery
}

internal sealed interface AutofillLaunch {
    data class Legacy(
        val request: AutofillFillRequest,
    ) : AutofillLaunch

    data object Accessibility : AutofillLaunch

    data class Credential(
        val action: CredentialResponseUiAction,
    ) : AutofillLaunch
}

private fun parseLaunch(source: Intent): AutofillLaunch? =
    when (source.action) {
        AutofillHostLaunchContract.ACTION_ACCESSIBILITY_FILL -> {
            AutofillLaunch.Accessibility
        }

        AutofillHostLaunchContract.ACTION_LEGACY_FILL -> {
            source.toAutofillFillRequest()?.let(AutofillLaunch::Legacy)
        }

        AutofillIntentExtras.ACTION_CREDENTIAL_GET_PASSWORD -> {
            AutofillLaunch.Credential(CredentialResponseUiAction.PasswordGet(source))
        }

        AutofillIntentExtras.ACTION_CREDENTIAL_UNLOCK -> {
            AutofillLaunch.Credential(CredentialResponseUiAction.Unlock(source))
        }

        AutofillIntentExtras.ACTION_CREDENTIAL_CREATE_PASSWORD -> {
            AutofillLaunch.Credential(CredentialResponseUiAction.PasswordCreate(source))
        }

        else -> {
            AutofillLaunch.Credential(CredentialResponseUiAction.UnknownAction)
        }
    }
