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
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.presentation.feature.shell.theme.AppTheme
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class AutofillFillActivity : FragmentActivity() {

    @Inject
    lateinit var authenticationHostRegistry: AuthenticationHostRegistry

    private val viewModel: AutofillFillViewModel by viewModels()
    private val resultFinishing = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val request = parseIntent(intent)

        setContent {
            AppTheme {
                AuthenticationHost(this, authenticationHostRegistry) {
                    AutofillFillRoute(
                        request = request,
                        viewModel = viewModel,
                        onResult = ::finishWithResult,
                    )
                }
            }
        }
    }

    private fun parseIntent(intent: Intent?): AutofillFillRequest {
        val raw = intent?.getStringExtra("autofill_ui_mode")
        val uiMode = AutofillPresentation.entries
            .firstOrNull { it.name == raw }
            ?: AutofillPresentation.SYSTEM_INLINE

        val isUnlockOnly = intent?.getBooleanExtra("unlock_only", false) ?: false
        val packageName = intent?.getStringExtra("package_name")
        val webDomain = intent?.getStringExtra("web_domain")
        val directEntryId = intent?.getStringExtra("vault_item_id")
        val candidateEntryIds = intent?.getStringArrayExtra("vault_item_ids")?.toList().orEmpty()
        val returnsDataset = intent?.getBooleanExtra(AutofillLaunchExtras.RETURN_DATASET, false)
            ?: false

        val editableIds = intent?.let {
            IntentCompat.getParcelableArrayExtra(it, "editable_ids", AutofillId::class.java)?.filterIsInstance<AutofillId>().orEmpty()
        }.orEmpty()
        val usernameIds = intent?.let {
            IntentCompat.getParcelableArrayListExtra(it, "username_ids", AutofillId::class.java).orEmpty()
        }.orEmpty()
        val passwordIds = intent?.let {
            IntentCompat.getParcelableArrayListExtra(it, "password_ids", AutofillId::class.java).orEmpty()
        }.orEmpty()
        val otpIds = intent?.let {
            IntentCompat.getParcelableArrayListExtra(it, "otp_ids", AutofillId::class.java).orEmpty()
        }.orEmpty()

        return AutofillFillRequest(
            uiMode = uiMode,
            isUnlockOnly = isUnlockOnly,
            packageName = packageName,
            webDomain = webDomain,
            directEntryId = directEntryId,
            candidateEntryIds = candidateEntryIds,
            returnsDataset = returnsDataset,
            editableIds = editableIds,
            usernameIds = usernameIds,
            passwordIds = passwordIds,
            otpIds = otpIds,
        )
    }

    private fun finishWithResult(payload: AutofillAuthenticationPayload?) {
        if (!resultFinishing.compareAndSet(false, true)) return
        val resultIntent = when (payload) {
            is AutofillAuthenticationPayload.Response -> Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, payload.value)
            }
            is AutofillAuthenticationPayload.DatasetResult -> Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, payload.value)
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET, true)
            }
            null -> null
        }

        if (resultIntent == null) {
            // No result but we finished normally (e.g. unlock with no matches).
            // Return OK with no data to keep the session alive for SaveInfo.
            setResult(RESULT_OK)
        } else {
            setResult(RESULT_OK, resultIntent)
        }

        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())

        val shouldCloseSession = payload == null || payload is AutofillAuthenticationPayload.DatasetResult
        lifecycleScope.launch {
            if (shouldCloseSession) viewModel.closeRequestSession()
            finish()
        }
    }

    override fun onDestroy() {
        if (!resultFinishing.get() && isFinishing) {
            lifecycleScope.launch { viewModel.closeRequestSession() }
        }
        super.onDestroy()
    }

}
