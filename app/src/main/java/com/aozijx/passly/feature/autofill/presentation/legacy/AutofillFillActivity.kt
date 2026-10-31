package com.aozijx.passly.feature.autofill.presentation.legacy

import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery
import com.aozijx.passly.feature.autofill.presentation.ActivityResultClaim
import com.aozijx.passly.feature.shell.presentation.theme.PersistedAppTheme
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

    private val viewModel: AutofillFillViewModel by viewModels()
    private val resultClaim = ActivityResultClaim()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        excludeFromAutofill()
        setResult(RESULT_CANCELED)
        val request = intent.toAutofillFillRequest() ?: run {
            finish()
            return
        }

        setContent {
            PersistedAppTheme {
                AuthenticationHost(this, authenticationHostRegistry, logger) {
                    AutofillFillRoute(
                        request = request,
                        viewModel = viewModel,
                        onResult = ::finishWithResult,
                    )
                }
            }
        }
    }

    private fun finishWithResult(result: AutofillFillResult) {
        val committed = try {
            resultClaim.commit {
                when (result) {
                    is AutofillFillResult.DiscoveryResponse -> {
                        setResult(
                            RESULT_OK,
                            Intent().apply {
                                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, result.value)
                            },
                        )
                        hideIme()
                        finish()
                    }
                    is AutofillFillResult.SelectedResponse ->
                        resultDelivery.commitResult(result.receipt) {
                            setResult(
                                RESULT_OK,
                                Intent().apply {
                                    putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, result.value)
                                },
                            )
                            hideIme()
                            finish()
                        }
                    is AutofillFillResult.SelectedDataset ->
                        resultDelivery.commitResult(result.receipt) {
                            setResult(
                                RESULT_OK,
                                Intent().apply {
                                    putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, result.value)
                                    putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET, true)
                                },
                            )
                            hideIme()
                            finish()
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

    private fun finishCancelledAfterDeliveryFailure() {
        val committed = try {
            resultClaim.commit {
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
        if (!resultClaim.isClaimed && isFinishing) {
            viewModel.onAction(AutofillFillUiAction.Cancel)
        }
        super.onDestroy()
    }

    private fun hideIme() {
        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())
    }
}
