package com.aozijx.passly.feature.autofill.presentation.legacy

import android.content.Intent
import android.os.Bundle
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.app.ActiveTaskClearRequests
import com.aozijx.passly.app.TaskClearActivityOwner
import com.aozijx.passly.app.ViewTaskClearCallbackScheduler
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.theme.PersistedAppTheme
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.feature.autofill.legacy.LegacyAutofillResultTarget
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AutofillFillActivity : FragmentActivity() {
    @Inject
    lateinit var authenticationHostRegistry: AuthenticationHostRegistry

    @Inject
    lateinit var resultDelivery: AutofillResultDelivery

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var responseFactory: LegacyResponseFactory

    @Inject
    lateinit var activeTaskClearRequests: ActiveTaskClearRequests

    @Inject
    lateinit var noticePublisher: AppNoticePublisher

    private val viewModel: AutofillFillViewModel by viewModels()
    private lateinit var taskClearActivityOwner: TaskClearActivityOwner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskClearActivityOwner =
            TaskClearActivityOwner(
                requests = activeTaskClearRequests,
                scheduler = ViewTaskClearCallbackScheduler(window.decorView),
                showReminder = { noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER)) },
                closeTask = ::finishAndRemoveTask,
            )
        excludeFromAutofill()
        setResult(RESULT_CANCELED)
        val request =
            intent.toAutofillFillRequest() ?: run {
                finish()
                return
            }

        setContent {
            PersistedAppTheme {
                AuthenticationHost(this, authenticationHostRegistry, logger) {
                    AutofillFillRoute(
                        request = request,
                        viewModel = viewModel,
                        onResult = { result -> finishWithResult(request, result) },
                    )
                }
            }
        }
    }

    private fun finishWithResult(
        request: AutofillFillRequest,
        result: AutofillFillResult,
    ) {
        val committed =
            try {
                viewModel.commitPlatformResult {
                    when (result) {
                        is AutofillFillResult.Discovery -> {
                            val response =
                                responseFactory
                                    .buildResult(
                                        this,
                                        result.outcome,
                                        request.toParsedStructure(),
                                        request.inlineRequest,
                                    ).response
                            if (response == null) {
                                viewModel.onAction(AutofillFillUiAction.Cancel)
                                setResult(RESULT_CANCELED)
                            } else {
                                setResult(
                                    RESULT_OK,
                                    Intent().apply {
                                        putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
                                    },
                                )
                            }
                            hideIme()
                            finish()
                        }

                        is AutofillFillResult.SelectedCredential -> {
                            val dataset =
                                responseFactory.buildDataset(
                                    payload = result.payload,
                                    usernameIds = request.usernameIds,
                                    passwordIds = request.passwordIds,
                                    otpIds = request.otpIds,
                                )
                            if (dataset == null) {
                                viewModel.onAction(AutofillFillUiAction.Cancel)
                                setResult(RESULT_CANCELED)
                                hideIme()
                                finish()
                            } else {
                                val platformResult =
                                    when (result.target) {
                                        LegacyAutofillResultTarget.DATASET -> {
                                            dataset
                                        }

                                        LegacyAutofillResultTarget.RESPONSE -> {
                                            FillResponse
                                                .Builder()
                                                .addDataset(dataset)
                                                .build()
                                        }
                                    }
                                resultDelivery.commitResult(result.receipt) {
                                    setResult(
                                        RESULT_OK,
                                        Intent().apply {
                                            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, platformResult)
                                            if (result.target == LegacyAutofillResultTarget.DATASET) {
                                                putExtra(
                                                    AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET,
                                                    true,
                                                )
                                            }
                                        },
                                    )
                                    hideIme()
                                    finish()
                                }
                            }
                        }

                        AutofillFillResult.Cancelled -> {
                            setResult(RESULT_CANCELED)
                            hideIme()
                            finish()
                        }
                    }
                }
            } catch (_: Exception) {
                viewModel.onAction(AutofillFillUiAction.Cancel)
                finishCancelledAfterDeliveryFailure()
                return
            }
        if (!committed) return
    }

    override fun onResume() {
        super.onResume()
        taskClearActivityOwner.onResumed()
    }

    override fun onPause() {
        taskClearActivityOwner.onPaused()
        super.onPause()
    }

    private fun finishCancelledAfterDeliveryFailure() {
        val committed =
            try {
                viewModel.commitPlatformResult {
                    setResult(RESULT_CANCELED)
                    hideIme()
                    finish()
                }
            } catch (_: Exception) {
                hideIme()
                finish()
                return
            }
        if (!committed) return
    }

    override fun onDestroy() {
        if (!viewModel.hasClaimedPlatformResult && isFinishing) {
            viewModel.onAction(AutofillFillUiAction.Cancel)
        }
        super.onDestroy()
    }

    private fun hideIme() {
        currentFocus?.clearFocus()
        WindowCompat
            .getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())
    }
}
