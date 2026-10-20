package com.aozijx.passly.presentation.feature.autofill.legacy

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
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import com.aozijx.passly.presentation.feature.shell.theme.AppTheme
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
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
    private val resultFinishing = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val request = intent.toAutofillFillRequest() ?: run {
            finish()
            return
        }

        setContent {
            AppTheme {
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
        if (!resultFinishing.compareAndSet(false, true)) return
        when (result) {
            is AutofillFillResult.DiscoveryResponse -> finishImmediately(
                Intent().apply {
                    putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, result.value)
                },
            )
            is AutofillFillResult.SelectedResponse -> finishAfterCommittedSelection(
                result.receipt,
                Intent().apply {
                    putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, result.value)
                },
            )
            is AutofillFillResult.SelectedDataset -> finishAfterCommittedSelection(
                result.receipt,
                Intent().apply {
                    putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, result.value)
                    putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET, true)
                },
            )
            AutofillFillResult.Cancelled -> finishCancelled()
        }
    }

    private fun finishImmediately(resultIntent: Intent) {
        setResult(RESULT_OK, resultIntent)
        hideIme()
        finish()
    }

    private fun finishCancelled() {
        setResult(RESULT_CANCELED)
        hideIme()
        finish()
    }

    private fun finishAfterCommittedSelection(
        receipt: AutofillSelectionReceipt,
        resultIntent: Intent,
    ) {
        resultDelivery.commitResult(receipt) {
            setResult(RESULT_OK, resultIntent)
            hideIme()
            finish()
        }
    }

    override fun onDestroy() {
        if (!resultFinishing.get() && isFinishing) {
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
