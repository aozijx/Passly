package com.aozijx.passly.presentation.feature.autofill.legacy

import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.presentation.feature.shell.theme.AppTheme
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery
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
        val request = parseIntent(intent) ?: run {
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

    private fun parseIntent(intent: Intent?): AutofillFillRequest? {
        val interactionId = intent?.getStringExtra(AutofillLaunchExtras.INTERACTION_ID)
            ?.takeIf(String::isNotBlank)
            ?.let(::AutofillInteractionId)
            ?: return null
        val packageName = intent.getStringExtra(AutofillLaunchExtras.PACKAGE_NAME)
            ?.takeIf(String::isNotBlank)
            ?: return null
        val raw = intent.getStringExtra(AutofillLaunchExtras.UI_MODE)
        val uiMode = AutofillPresentation.entries
            .firstOrNull { it.name == raw }
            ?: AutofillPresentation.SYSTEM_INLINE

        val webDomain = intent.getStringExtra(AutofillLaunchExtras.WEB_DOMAIN)
        val directEntryId = intent.getStringExtra(AutofillLaunchExtras.ENTRY_ID)
            ?.takeIf(String::isNotBlank)
            ?.let(::EntryId)
        val returnsDataset = intent.getBooleanExtra(AutofillLaunchExtras.RETURN_DATASET, false)
        val editableIds = intent.let {
            IntentCompat.getParcelableArrayExtra(
                it,
                AutofillLaunchExtras.EDITABLE_IDS,
                AutofillId::class.java,
            )?.filterIsInstance<AutofillId>().orEmpty()
        }.orEmpty()
        val usernameIds = intent.let {
            IntentCompat.getParcelableArrayListExtra(
                it,
                AutofillLaunchExtras.USERNAME_IDS,
                AutofillId::class.java,
            ).orEmpty()
        }.orEmpty()
        val passwordIds = intent.let {
            IntentCompat.getParcelableArrayListExtra(
                it,
                AutofillLaunchExtras.PASSWORD_IDS,
                AutofillId::class.java,
            ).orEmpty()
        }.orEmpty()
        val otpIds = intent.let {
            IntentCompat.getParcelableArrayListExtra(
                it,
                AutofillLaunchExtras.OTP_IDS,
                AutofillId::class.java,
            ).orEmpty()
        }.orEmpty()

        return AutofillFillRequest(
            interactionId = interactionId,
            uiMode = uiMode,
            packageName = packageName,
            webDomain = webDomain,
            directEntryId = directEntryId,
            returnsDataset = returnsDataset,
            editableIds = editableIds,
            usernameIds = usernameIds,
            passwordIds = passwordIds,
            otpIds = otpIds,
        )
    }

    private fun finishWithResult(result: AutofillFillResult) {
        if (!resultFinishing.compareAndSet(false, true)) return
        val resultIntent = when (result) {
            is AutofillFillResult.Response -> Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, result.value)
            }
            is AutofillFillResult.DatasetResult -> Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, result.value)
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET, true)
            }
            AutofillFillResult.Cancelled -> null
        }

        val receipt = result.receipt
        when {
            resultIntent == null -> setResult(RESULT_CANCELED)
            receipt == null -> setResult(RESULT_OK, resultIntent)
            else -> resultDelivery.deliverResult(receipt) {
                setResult(RESULT_OK, resultIntent)
            }
        }

        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())

        finish()
    }

    override fun onDestroy() {
        if (!resultFinishing.get() && isFinishing) {
            viewModel.onAction(AutofillFillUiAction.Cancel)
        }
        super.onDestroy()
    }

}
