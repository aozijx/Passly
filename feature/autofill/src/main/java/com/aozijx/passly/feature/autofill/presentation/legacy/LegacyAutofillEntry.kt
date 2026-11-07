package com.aozijx.passly.feature.autofill.presentation.legacy

import android.content.Intent
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aozijx.passly.feature.autofill.legacy.LegacyAutofillResultTarget
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.presentation.AutofillEntryPoint
import com.aozijx.passly.feature.autofill.presentation.AutofillHostCompletion
import com.aozijx.passly.feature.autofill.presentation.AutofillLaunch
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery

@Composable
internal fun LegacyAutofillEntry(
    launch: AutofillLaunch.Legacy,
    activity: ComponentActivity,
    dependencies: AutofillEntryPoint,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: AutofillFillViewModel = hiltViewModel(activity)

    DisposableEffect(activity, lifecycleOwner, launch) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> if (!activity.isChangingConfigurations) viewModel.cancelIfWaitingForSelection()
                    Lifecycle.Event.ON_DESTROY -> if (activity.isFinishing && !viewModel.hasClaimedPlatformResult) viewModel.cancel()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AutofillFillRoute(
        request = launch.request,
        viewModel = viewModel,
        onResult = { result ->
            deliverLegacyResult(
                launch.request,
                result,
                activity,
                viewModel,
                dependencies.responseFactory(),
                dependencies.resultDelivery(),
                onComplete,
            )
        },
    )
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
                    val response =
                        responseFactory.buildAuthenticationResponse(
                            activity,
                            result.outcome,
                            request.toParsedStructure(),
                            request.inlineRequest,
                        )
                    if (response == null) {
                        viewModel.cancel()
                        onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
                    } else {
                        onComplete(
                            AutofillHostCompletion(
                                android.app.Activity.RESULT_OK,
                                Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response),
                            ),
                        )
                    }
                }

                is AutofillFillResult.SelectedCredential -> {
                    val dataset = responseFactory.buildDataset(result.payload, request.usernameIds, request.passwordIds, request.otpIds)
                    if (dataset == null) {
                        viewModel.cancel()
                        onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
                    } else {
                        val platformResult =
                            when (result.target) {
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

                AutofillFillResult.Cancelled -> {
                    onComplete(AutofillHostCompletion(android.app.Activity.RESULT_CANCELED))
                }
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
