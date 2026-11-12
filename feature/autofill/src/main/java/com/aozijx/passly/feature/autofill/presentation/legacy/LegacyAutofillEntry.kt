package com.aozijx.passly.feature.autofill.presentation.legacy

import android.content.Intent
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.feature.autofill.legacy.LegacyAutofillResultTarget
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyDatasetFactory
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
    entryIcon: @Composable (EntryListItem, Modifier) -> Unit,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: AutofillFillViewModel = hiltViewModel(activity)
    val logger = dependencies.logger()

    DisposableEffect(activity, lifecycleOwner, launch) {
        var hostAbandoned = false

        fun abandonPendingResult() {
            if (hostAbandoned || viewModel.hasClaimedPlatformResult) return
            hostAbandoned = true
            viewModel.cancel(launch.request.interactionId)
            logger.log(LogStatements.Autofill.hostAbandoned(AutofillInteractionSource.LEGACY))
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

    AutofillFillRoute(
        request = launch.request,
        viewModel = viewModel,
        entryIcon = entryIcon,
        onResult = { result ->
            deliverLegacyResult(
                launch.request,
                result,
                activity,
                viewModel,
                dependencies.responseFactory(),
                dependencies.resultDelivery(),
                logger,
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
    logger: Logger,
    onComplete: (AutofillHostCompletion) -> Unit,
) {
    try {
        viewModel.commitPlatformResult { markPlatformCommitted ->
            when (result) {
                is AutofillFillResult.Discovery -> {
                    val response =
                        responseFactory
                            .buildResult(
                                activity,
                                result.outcome,
                                packageName = request.packageName,
                                webDomain = request.webDomain,
                                editableIds = request.editableIds,
                                inlineRequest = request.inlineRequest,
                            ).response
                    if (response == null) {
                        viewModel.cancel()
                        onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_CANCELED))
                        markPlatformCommitted()
                    } else {
                        onComplete(
                            AutofillHostCompletion.ActivityResult(
                                android.app.Activity.RESULT_OK,
                                Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response),
                            ),
                        )
                        markPlatformCommitted()
                    }
                }

                is AutofillFillResult.SelectedCredential -> {
                    val dataset =
                        LegacyDatasetFactory.createFillDatasetForRoles(
                            usernameIds = request.usernameIds,
                            passwordIds = request.passwordIds,
                            otpIds = request.otpIds,
                            username = result.payload.username,
                            password = result.payload.password,
                            totpCode = result.payload.otpCode,
                        )
                    if (dataset == null) {
                        viewModel.cancel()
                        onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_CANCELED))
                        markPlatformCommitted()
                    } else {
                        val platformResult =
                            when (result.target) {
                                LegacyAutofillResultTarget.DATASET -> dataset
                                LegacyAutofillResultTarget.RESPONSE -> FillResponse.Builder().addDataset(dataset).build()
                            }
                        resultDelivery.commitResult(result.receipt) { markDeliveryCommitted ->
                            val resultIntent = Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, platformResult)
                            if (result.target == LegacyAutofillResultTarget.DATASET) {
                                resultIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET, true)
                            }
                            onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_OK, resultIntent))
                            markDeliveryCommitted()
                            markPlatformCommitted()
                        }
                    }
                }

                AutofillFillResult.Cancelled -> {
                    onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_CANCELED))
                    markPlatformCommitted()
                }
            }
        }
    } catch (error: Exception) {
        viewModel.cancel()
        logger.log(LogStatements.Autofill.platformResultCommitFailed(AutofillInteractionSource.LEGACY, error))
        try {
            viewModel.commitPlatformResult { markPlatformCommitted ->
                onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_CANCELED))
                markPlatformCommitted()
            }
        } catch (_: Exception) {
            onComplete(AutofillHostCompletion.ActivityResult(android.app.Activity.RESULT_CANCELED))
        }
    }
}
