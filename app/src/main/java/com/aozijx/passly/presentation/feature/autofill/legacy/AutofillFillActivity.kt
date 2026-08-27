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
import com.aozijx.passly.app.security.authentication.AuthenticationHost
import com.aozijx.passly.core.ui.theme.AppTheme
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.autofill.model.ResolvedCandidate
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
import com.aozijx.passly.presentation.ui.autofill.AutofillCandidateBottomSheet
import com.aozijx.passly.presentation.ui.autofill.AutofillCandidateItem
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

        setContent {
            AppTheme {
                AuthenticationHost(this, authenticationHostRegistry) {}
            }
        }

        val request = parseIntent(intent)

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AutofillFillUiState.Initial -> Unit
                    is AutofillFillUiState.Loading -> { }
                    is AutofillFillUiState.ShowCandidates -> {
                        showBottomSheet(state.candidates)
                    }
                    is AutofillFillUiState.Result -> {
                        finishWithResult(state.payload)
                    }
                    is AutofillFillUiState.Error -> {
                        finishWithResult(null)
                    }
                }
            }
        }

        viewModel.onAction(AutofillFillUiAction.Initialize(request))
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

    private fun showBottomSheet(candidates: List<ResolvedCandidate>) {
        val candidatesById = candidates.associateBy { it.entry.id.value }
        setContent {
            AppTheme {
                AuthenticationHost(this, authenticationHostRegistry) {
                    AutofillCandidateBottomSheet(
                        candidates = candidates.map { candidate -> candidate.toUiItem() },
                        onCandidateSelected = { candidateId ->
                            candidatesById[candidateId]?.let { candidate ->
                                viewModel.onAction(AutofillFillUiAction.CandidateSelected(candidate))
                            }
                        },
                        onCancel = { finishWithResult(null) }
                    )
                }
            }
        }
    }

    private fun ResolvedCandidate.toUiItem() = AutofillCandidateItem(
        id = entry.id.value,
        iconName = entry.profile.icon.name,
        iconCustomPath = entry.profile.icon.customReference,
        associatedAppPackage = entry.profile.associations.applicationIds.firstOrNull(),
        entryTypeKey = entry.entryType.name,
        title = entry.title,
        username = entry.username,
        associatedDomain = entry.associatedDomain,
    )

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
