package com.aozijx.passly.feature.autofill.presentation.credential

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.feature.autofill.credential.CredentialRequestParser
import com.aozijx.passly.feature.autofill.presentation.AutofillEntryPoint
import com.aozijx.passly.feature.autofill.presentation.AutofillHostCompletion
import com.aozijx.passly.feature.autofill.presentation.AutofillLaunch
import com.aozijx.passly.feature.autofill.presentation.result.AutofillActivityResultClaim
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery

@Composable
internal fun CredentialAutofillEntry(
    launch: AutofillLaunch.Credential,
    dependencies: AutofillEntryPoint,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        LaunchedEffect(launch) { onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_CANCELED)) }
        return
    }

    CredentialAutofillEntryApi34(launch, dependencies, onComplete)
}

@Composable
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun CredentialAutofillEntryApi34(
    launch: AutofillLaunch.Credential,
    dependencies: AutofillEntryPoint,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    val activity =
        LocalActivity.current as? ComponentActivity
            ?: error("CredentialAutofillEntry requires a ComponentActivity host")
    val lifecycleOwner = LocalLifecycleOwner.current
    val logger = dependencies.logger()
    val resultDelivery = dependencies.resultDelivery()
    val viewModel: CredentialResponseViewModel = hiltViewModel(activity)
    val resultClaim = remember { AutofillActivityResultClaim() }
    val interactionId = remember(launch) { launch.action.pendingInteractionId() }

    LaunchedEffect(launch) {
        if (launch.action == CredentialResponseUiAction.UnknownAction) {
            logger.withRootTrace(Operations.AutofillCredentialRequest) {
                logger.log(LogStatements.Autofill.actionIgnored())
            }
        }
    }

    DisposableEffect(activity, lifecycleOwner, resultClaim, interactionId) {
        var hostAbandoned = false

        fun abandonPendingResult() {
            if (hostAbandoned || resultClaim.isClaimed) return
            hostAbandoned = true
            viewModel.cancelActiveInteraction(interactionId)
            logger.log(LogStatements.Autofill.hostAbandoned(AutofillInteractionSource.CREDENTIAL_MANAGER))
        }
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> if (!activity.isChangingConfigurations) abandonPendingResult()
                    Lifecycle.Event.ON_DESTROY -> if (activity.isFinishing) abandonPendingResult()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CredentialResponseRoute(
        action = launch.action,
        viewModel = viewModel,
        onComplete = { completion -> deliverCredentialResult(completion, viewModel, resultDelivery, resultClaim, logger, onComplete) },
        onUnrecoverable = { cancelCredential(viewModel, resultClaim, onComplete) },
    )
}

private fun CredentialResponseUiAction.pendingInteractionId(): AutofillInteractionId? =
    when (this) {
        is CredentialResponseUiAction.PasswordGet -> CredentialRequestParser.parseInteractionId(sourceIntent)
        is CredentialResponseUiAction.Unlock -> CredentialRequestParser.parseInteractionId(sourceIntent)
        is CredentialResponseUiAction.PasswordCreate -> CredentialRequestParser.parseInteractionId(sourceIntent)
        CredentialResponseUiAction.UnknownAction -> null
    }

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun deliverCredentialResult(
    completion: CredentialResponseCompletion,
    viewModel: CredentialResponseViewModel,
    resultDelivery: AutofillResultDelivery,
    resultClaim: AutofillActivityResultClaim,
    logger: Logger,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    try {
        val committed =
            resultClaim.commit { markPlatformCommitted ->
                when (completion) {
                    is CredentialResponseCompletion.Immediate -> {
                        onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_OK, completion.resultIntent))
                        markPlatformCommitted()
                    }

                    is CredentialResponseCompletion.CommittedSelection -> {
                        resultDelivery.commitResult(completion.receipt) { markDeliveryCommitted ->
                            onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_OK, completion.resultIntent))
                            markDeliveryCommitted()
                            markPlatformCommitted()
                        }
                    }
                }
            }
        if (committed) viewModel.onPlatformResultDelivered(completion)
    } catch (error: Exception) {
        logger.log(LogStatements.Autofill.platformResultCommitFailed(AutofillInteractionSource.CREDENTIAL_MANAGER, error))
        cancelCredential(viewModel, resultClaim, onComplete)
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun cancelCredential(
    viewModel: CredentialResponseViewModel,
    resultClaim: AutofillActivityResultClaim,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    try {
        if (resultClaim.commit { markPlatformCommitted ->
                onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_CANCELED))
                markPlatformCommitted()
            }
        ) {
            viewModel.cancelActiveInteraction()
        }
    } catch (_: Exception) {
        viewModel.cancelActiveInteraction()
        onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_CANCELED))
    }
}
