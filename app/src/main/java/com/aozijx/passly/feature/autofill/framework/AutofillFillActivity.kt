package com.aozijx.passly.feature.autofill.framework

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
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.ui.theme.AppTheme
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.auth.ui.host.AuthenticationHost
import com.aozijx.passly.feature.autofill.AutofillCandidateBottomSheet
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

        // 观察 ViewModel 状态
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AutofillFillUiState.Initial -> Unit
                    is AutofillFillUiState.Loading -> {
                        // 可显示加载指示（不阻塞 UI）
                    }

                    is AutofillFillUiState.ShowCandidates -> {
                        showBottomSheet(state.candidates)
                    }

                    is AutofillFillUiState.Result -> {
                        finishWithResult(state.payload)
                    }

                    is AutofillFillUiState.Error -> {
                        // 显示错误 Toast 或直接取消
                        finishWithResult(null)
                    }
                }
            }
        }

        // 启动处理
        viewModel.initialize(request)
    }

    private fun parseIntent(intent: Intent?): AutofillFillRequest {
        val raw = intent?.getStringExtra("autofill_ui_mode")
        val uiMode = AutofillPresentation.entries
            .firstOrNull { it.name == raw }
            ?: AutofillPresentation.SYSTEM_INLINE

        val isUnlockOnly = intent?.getBooleanExtra("unlock_only", false) ?: false
        val usernameId = intent?.let {
            IntentCompat.getParcelableExtra(
                it,
                "username_id",
                AutofillId::class.java
            )
        }
        val passwordId = intent?.let {
            IntentCompat.getParcelableExtra(
                it,
                "password_id",
                AutofillId::class.java
            )
        }
        val otpId =
            intent?.let { IntentCompat.getParcelableExtra(it, "otp_id", AutofillId::class.java) }
        val packageName = intent?.getStringExtra("package_name")
        val webDomain = intent?.getStringExtra("web_domain")
        val directEntryId = intent?.getStringExtra("vault_item_id")
        val candidateEntryIds = intent?.getStringArrayExtra("vault_item_ids")?.toList().orEmpty()
        val returnsDataset = intent?.getBooleanExtra(EXTRA_RETURN_DATASET, false) ?: false

        return AutofillFillRequest(
            uiMode = uiMode,
            isUnlockOnly = isUnlockOnly,
            usernameId = usernameId,
            passwordId = passwordId,
            otpId = otpId,
            packageName = packageName,
            webDomain = webDomain,
            directEntryId = directEntryId,
            candidateEntryIds = candidateEntryIds,
            returnsDataset = returnsDataset
        )
    }

    private fun showBottomSheet(candidates: List<ResolvedCandidate>) {
        setContent {
            AppTheme {
                AuthenticationHost(this, authenticationHostRegistry) {
                    AutofillCandidateBottomSheet(
                        candidates = candidates,
                        onCandidateSelected = { candidate ->
                            viewModel.selectCandidate(candidate)
                        },
                        onCancel = { finishWithResult(null) }
                    )
                }
            }
        }
    }

    private fun finishWithResult(payload: AutofillAuthenticationPayload?) {
        if (!resultFinishing.compareAndSet(false, true)) return
        val resultIntent = when (payload) {
            is AutofillAuthenticationPayload.Response -> Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, payload.value)
            }

            is AutofillAuthenticationPayload.DatasetResult -> Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, payload.value)
                putExtra(
                    AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET,
                    true
                )
            }

            null -> null
        }

        if (resultIntent == null) setResult(RESULT_CANCELED)
        else setResult(RESULT_OK, resultIntent)

        // Compose password fields can keep the IME served view attached while
        // this transparent Activity is being destroyed. Hide it explicitly
        // before returning control to the client application.
        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())

        lifecycleScope.launch {
            viewModel.closeRequestSession()
            finish()
        }
    }

    companion object {
        internal const val EXTRA_RETURN_DATASET = "autofill_return_dataset"
    }
}
