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
        LaunchedEffect(launch) { onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED)) }
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

    LaunchedEffect(launch) {
        if (launch.action == CredentialResponseUiAction.UnknownAction) {
            logger.withRootTrace(Operations.AutofillCredentialRequest) {
                logger.log(LogStatements.Autofill.actionIgnored())
            }
        }
    }

    DisposableEffect(activity, lifecycleOwner, resultClaim) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY && activity.isFinishing && !resultClaim.isClaimed) {
                    viewModel.cancelActiveInteraction()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CredentialResponseRoute(
        action = launch.action,
        viewModel = viewModel,
        onComplete = { completion -> deliverCredentialResult(completion, viewModel, resultDelivery, resultClaim, onComplete) },
        onUnrecoverable = { cancelCredential(viewModel, resultClaim, onComplete) },
    )
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun deliverCredentialResult(
    completion: CredentialResponseCompletion,
    viewModel: CredentialResponseViewModel,
    resultDelivery: AutofillResultDelivery,
    resultClaim: AutofillActivityResultClaim,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    try {
        val committed =
            resultClaim.commit {
                when (completion) {
                    is CredentialResponseCompletion.Immediate -> {
                        onComplete(AutofillHostCompletion(android.app.Activity.RESULT_OK, completion.resultIntent))
                    }

                    is CredentialResponseCompletion.CommittedSelection -> {
                        resultDelivery.commitResult(completion.receipt) {
                            onComplete(AutofillHostCompletion(android.app.Activity.RESULT_OK, completion.resultIntent))
                        }
                    }
                }
            }
        if (committed) viewModel.onPlatformResultDelivered(completion)
    } catch (_: Exception) {
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
        if (resultClaim.commit { onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED)) }) {
            viewModel.cancelActiveInteraction()
        }
    } catch (_: Exception) {
        viewModel.cancelActiveInteraction()
        onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
    }
}
