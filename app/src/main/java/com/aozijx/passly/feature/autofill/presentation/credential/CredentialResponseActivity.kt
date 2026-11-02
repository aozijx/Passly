package com.aozijx.passly.feature.autofill.presentation.credential

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.app.ActiveTaskClearRequests
import com.aozijx.passly.app.TaskClearActivityOwner
import com.aozijx.passly.app.ViewTaskClearCallbackScheduler
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.theme.PersistedAppTheme
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.feature.autofill.credential.service.ModernCredentialService
import com.aozijx.passly.feature.autofill.presentation.ActivityResultClaim
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@AndroidEntryPoint
class CredentialResponseActivity : AppCompatActivity() {
    @Inject
    lateinit var authenticationHostRegistry: AuthenticationHostRegistry

    @Inject
    lateinit var resultDelivery: AutofillResultDelivery

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var activeTaskClearRequests: ActiveTaskClearRequests

    @Inject
    lateinit var noticePublisher: AppNoticePublisher

    private val viewModel: CredentialResponseViewModel by viewModels()
    private val resultClaim = ActivityResultClaim()
    private lateinit var taskClearActivityOwner: TaskClearActivityOwner

    companion object {
        private const val TAG = "CredResponse"
    }

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

        val launch = responseLaunch(intent)

        setContent {
            PersistedAppTheme {
                AuthenticationHost(this, authenticationHostRegistry, logger) {
                    CredentialResponseRoute(
                        action = launch.action,
                        viewModel = viewModel,
                        onComplete = ::finishWithResult,
                        onUnrecoverable = ::finishWithError,
                    )
                }
            }
        }
    }

    private fun responseLaunch(sourceIntent: Intent): CredentialResponseLaunch =
        when (val action = sourceIntent.action) {
            ModernCredentialService.ACTION_GET_PASSWORD -> {
                CredentialResponseLaunch(
                    action = CredentialResponseUiAction.PasswordGet(sourceIntent),
                )
            }

            ModernCredentialService.ACTION_UNLOCK -> {
                CredentialResponseLaunch(
                    action = CredentialResponseUiAction.Unlock(sourceIntent),
                )
            }

            ModernCredentialService.ACTION_CREATE_PASSWORD -> {
                CredentialResponseLaunch(
                    action = CredentialResponseUiAction.PasswordCreate(sourceIntent),
                )
            }

            else -> {
                lifecycleScope.launch {
                    logger.withRootTrace(Operations.AutofillCredentialRequest) {
                        logger.log(LogStatements.Autofill.actionIgnored())
                    }
                }
                CredentialResponseLaunch(
                    action = CredentialResponseUiAction.UnknownAction,
                )
            }
        }

    private fun finishWithResult(completion: CredentialResponseCompletion) {
        val committed =
            try {
                resultClaim.commit {
                    // PendingIntentHandler requires RESULT_OK for valid responses and exceptions.
                    when (completion) {
                        is CredentialResponseCompletion.Immediate -> {
                            setResult(RESULT_OK, completion.resultIntent)
                            hideIme()
                            finish()
                        }

                        is CredentialResponseCompletion.CommittedSelection -> {
                            resultDelivery.commitResult(completion.receipt) {
                                setResult(RESULT_OK, completion.resultIntent)
                                hideIme()
                                finish()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                finishCancelled()
                return
            }
        if (!committed) return
        viewModel.onPlatformResultDelivered(completion)
    }

    private fun finishWithError() {
        finishCancelled()
    }

    private fun finishCancelled() {
        val committed =
            try {
                resultClaim.commit { setResult(RESULT_CANCELED) }
            } catch (_: Exception) {
                viewModel.cancelActiveInteraction()
                finish()
                return
            }
        if (!committed) return
        hideIme()
        viewModel.cancelActiveInteraction()
        finish()
    }

    override fun onDestroy() {
        // Back/system-finish paths did not deliver a response. Configuration
        // changes keep the same ViewModel and must not cancel the interaction.
        if (!resultClaim.isClaimed && isFinishing) {
            viewModel.cancelActiveInteraction()
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        taskClearActivityOwner.onResumed()
    }

    override fun onPause() {
        taskClearActivityOwner.onPaused()
        super.onPause()
    }

    private fun hideIme() {
        currentFocus?.clearFocus()
        WindowCompat
            .getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())
    }

    private data class CredentialResponseLaunch(
        val action: CredentialResponseUiAction,
    )
}
