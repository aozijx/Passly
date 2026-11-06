package com.aozijx.passly.feature.autofill.presentation

import android.content.Intent
import android.os.Build
import android.view.autofill.AutofillManager
import android.service.autofill.FillResponse
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.ComponentActivity
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.feature.autofill.legacy.LegacyAutofillResultTarget
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.platform.AutofillHostLaunchContract
import com.aozijx.passly.feature.autofill.platform.AutofillIntentExtras
import com.aozijx.passly.feature.autofill.presentation.accessibility.AccessibilityFillRoute
import com.aozijx.passly.feature.autofill.presentation.accessibility.AccessibilityFillViewModel
import com.aozijx.passly.feature.autofill.presentation.credential.CredentialResponseCompletion
import com.aozijx.passly.feature.autofill.presentation.credential.CredentialResponseRoute
import com.aozijx.passly.feature.autofill.presentation.credential.CredentialResponseUiAction
import com.aozijx.passly.feature.autofill.presentation.credential.CredentialResponseViewModel
import com.aozijx.passly.feature.autofill.presentation.legacy.AutofillFillRequest
import com.aozijx.passly.feature.autofill.presentation.legacy.AutofillFillResult
import com.aozijx.passly.feature.autofill.presentation.legacy.AutofillFillRoute
import com.aozijx.passly.feature.autofill.presentation.legacy.AutofillFillViewModel
import com.aozijx.passly.feature.autofill.presentation.legacy.toParsedStructure
import com.aozijx.passly.feature.autofill.presentation.legacy.toAutofillFillRequest
import com.aozijx.passly.feature.autofill.presentation.result.AutofillActivityResultClaim
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/** Minimal platform boundary returned to the App-owned Activity. */
data class AutofillHostCompletion(
    val resultCode: Int? = null,
    val data: Intent? = null,
    val hideIme: Boolean = true,
)

enum class AutofillHostPresentation {
    LEGACY_FILL,
    ACCESSIBILITY_FILL,
    CREDENTIAL,
}

/** App uses this small classification before `super.onCreate` to select its theme and window flags. */
fun autofillHostPresentation(action: String?): AutofillHostPresentation =
    when (action) {
        AutofillHostLaunchContract.ACTION_LEGACY_FILL -> AutofillHostPresentation.LEGACY_FILL
        AutofillHostLaunchContract.ACTION_ACCESSIBILITY_FILL -> AutofillHostPresentation.ACCESSIBILITY_FILL
        else -> AutofillHostPresentation.CREDENTIAL
    }

/** Owns Autofill launch parsing, Feature ViewModels, route selection, result construction, and cleanup. */
@Composable
fun AutofillFeatureEntry(
    sourceIntent: Intent,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    val activity = LocalContext.current as? ComponentActivity
        ?: error("AutofillFeatureEntry requires a ComponentActivity host")
    val lifecycleOwner = LocalLifecycleOwner.current
    val dependencies = EntryPointAccessors.fromActivity(activity, AutofillEntryPoint::class.java)
    val logger = dependencies.logger()
    val responseFactory = dependencies.responseFactory()
    val resultDelivery = dependencies.resultDelivery()
    val legacyViewModel: AutofillFillViewModel = hiltViewModel(activity)
    val accessibilityViewModel: AccessibilityFillViewModel = hiltViewModel(activity)
    val credentialViewModel: CredentialResponseViewModel = hiltViewModel(activity)
    val launch = androidx.compose.runtime.remember(sourceIntent) { parseLaunch(sourceIntent) }
    val credentialResultClaim = androidx.compose.runtime.remember { AutofillActivityResultClaim() }

    DisposableEffect(activity, lifecycleOwner, launch) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (!activity.isChangingConfigurations && launch is AutofillLaunch.Legacy) {
                        legacyViewModel.cancelIfWaitingForSelection()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> if (activity.isFinishing) {
                    when (launch) {
                        is AutofillLaunch.Legacy -> if (!legacyViewModel.hasClaimedPlatformResult) legacyViewModel.cancel()
                        AutofillLaunch.Accessibility -> accessibilityViewModel.cancel()
                        is AutofillLaunch.Credential -> if (!credentialResultClaim.isClaimed) credentialViewModel.cancelActiveInteraction()
                        null -> Unit
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(sourceIntent, launch) {
        when (launch) {
            null -> onComplete(AutofillHostCompletion(resultCode = android.app.Activity.RESULT_CANCELED))
            is AutofillLaunch.Legacy -> Unit
            AutofillLaunch.Accessibility -> Unit
            is AutofillLaunch.Credential -> if (launch.action == CredentialResponseUiAction.UnknownAction) {
                logger.withRootTrace(Operations.AutofillCredentialRequest) {
                    logger.log(LogStatements.Autofill.actionIgnored())
                }
            }
        }
    }

    when (val current = launch) {
        null -> Unit
        is AutofillLaunch.Legacy -> {
            AutofillFillRoute(
                request = current.request,
                viewModel = legacyViewModel,
                onResult = { result ->
                    deliverLegacyResult(current.request, result, activity, legacyViewModel, responseFactory, resultDelivery, onComplete)
                },
            )
        }
        AutofillLaunch.Accessibility -> AccessibilityFillRoute(
            viewModel = accessibilityViewModel,
            onComplete = { onComplete(AutofillHostCompletion(resultCode = null, hideIme = false)) },
        )
        is AutofillLaunch.Credential -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            CredentialResponseRoute(
                action = current.action,
                viewModel = credentialViewModel,
                onComplete = { completion ->
                    deliverCredentialResult(completion, credentialViewModel, resultDelivery, credentialResultClaim, onComplete)
                },
                onUnrecoverable = {
                    cancelCredential(credentialViewModel, credentialResultClaim, onComplete)
                },
            )
        } else {
            LaunchedEffect(current) { onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED)) }
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

private sealed interface AutofillLaunch {
    data class Legacy(val request: AutofillFillRequest) : AutofillLaunch
    data object Accessibility : AutofillLaunch
    data class Credential(val action: CredentialResponseUiAction) : AutofillLaunch
}

private fun parseLaunch(source: Intent): AutofillLaunch? =
    when (source.action) {
        AutofillHostLaunchContract.ACTION_ACCESSIBILITY_FILL -> AutofillLaunch.Accessibility
        AutofillHostLaunchContract.ACTION_LEGACY_FILL -> source.toAutofillFillRequest()?.let(AutofillLaunch::Legacy)
        AutofillIntentExtras.ACTION_CREDENTIAL_GET_PASSWORD -> AutofillLaunch.Credential(CredentialResponseUiAction.PasswordGet(source))
        AutofillIntentExtras.ACTION_CREDENTIAL_UNLOCK -> AutofillLaunch.Credential(CredentialResponseUiAction.Unlock(source))
        AutofillIntentExtras.ACTION_CREDENTIAL_CREATE_PASSWORD -> AutofillLaunch.Credential(CredentialResponseUiAction.PasswordCreate(source))
        else -> AutofillLaunch.Credential(CredentialResponseUiAction.UnknownAction)
    }

private fun deliverLegacyResult(
    request: AutofillFillRequest,
    result: AutofillFillResult,
    activity: ComponentActivity,
    viewModel: AutofillFillViewModel,
    responseFactory: LegacyResponseFactory,
    resultDelivery: AutofillResultDelivery,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    try {
        viewModel.commitPlatformResult {
            when (result) {
                is AutofillFillResult.Discovery -> {
                    val response = responseFactory.buildAuthenticationResponse(
                        activity,
                        result.outcome,
                        request.toParsedStructure(),
                        request.inlineRequest,
                    )
                    if (response == null) {
                        viewModel.cancel()
                        onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
                    } else {
                        onComplete(AutofillHostCompletion(
                            android.app.Activity.RESULT_OK,
                            Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response),
                        ))
                    }
                }
                is AutofillFillResult.SelectedCredential -> {
                    val dataset = responseFactory.buildDataset(result.payload, request.usernameIds, request.passwordIds, request.otpIds)
                    if (dataset == null) {
                        viewModel.cancel()
                        onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
                    } else {
                        val platformResult = when (result.target) {
                            LegacyAutofillResultTarget.DATASET -> dataset
                            LegacyAutofillResultTarget.RESPONSE -> FillResponse.Builder().addDataset(dataset).build()
                        }
                        resultDelivery.commitResult(result.receipt) {
                            val resultIntent = Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, platformResult)
                            if (result.target == LegacyAutofillResultTarget.DATASET) {
                                resultIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET, true)
                            }
                            onComplete(AutofillHostCompletion(android.app.Activity.RESULT_OK, resultIntent))
                        }
                    }
                }
                AutofillFillResult.Cancelled -> onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
            }
        }
    } catch (_: Exception) {
        viewModel.cancel()
        try {
            viewModel.commitPlatformResult {
                onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
            }
        } catch (_: Exception) {
            onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
        }
    }
}

private fun deliverCredentialResult(
    completion: CredentialResponseCompletion,
    viewModel: CredentialResponseViewModel,
    resultDelivery: AutofillResultDelivery,
    resultClaim: AutofillActivityResultClaim,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    try {
        val committed = resultClaim.commit {
            when (completion) {
                is CredentialResponseCompletion.Immediate -> onComplete(AutofillHostCompletion(android.app.Activity.RESULT_OK, completion.resultIntent))
                is CredentialResponseCompletion.CommittedSelection -> resultDelivery.commitResult(completion.receipt) {
                    onComplete(AutofillHostCompletion(android.app.Activity.RESULT_OK, completion.resultIntent))
                }
            }
        }
        if (committed) viewModel.onPlatformResultDelivered(completion)
    } catch (_: Exception) {
        cancelCredential(viewModel, resultClaim, onComplete)
    }
}

private fun cancelCredential(
    viewModel: CredentialResponseViewModel,
    resultClaim: AutofillActivityResultClaim,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    try {
        if (resultClaim.commit { onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED)) }) {
            viewModel.cancelActiveInteraction()
        }
    } catch (_: Exception) {
        viewModel.cancelActiveInteraction()
        onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
    }
}
