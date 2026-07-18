package com.aozijx.passly.feature.autofill.framework

import android.content.Intent
import android.os.Bundle
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.model.settings.AutofillUiMode
import com.aozijx.passly.feature.autofill.AutofillCandidateBottomSheet
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import com.aozijx.passly.ui.authentication.AuthenticationHost
import com.aozijx.passly.ui.common.FragmentActivityBiometricLauncher
import com.aozijx.passly.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutofillFillActivity : FragmentActivity() {

    @Inject
    lateinit var authenticationHostRegistry: AuthenticationHostRegistry

    private val viewModel: AutofillFillViewModel by viewModels()

    private val biometricLauncher by lazy { FragmentActivityBiometricLauncher(this) }

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
                    is AutofillFillViewModel.UiState.Initial -> Unit
                    is AutofillFillViewModel.UiState.Loading -> {
                        // 可显示加载指示（不阻塞 UI）
                    }

                    is AutofillFillViewModel.UiState.ShowCandidates -> {
                        showBottomSheet(state.candidates)
                    }

                    is AutofillFillViewModel.UiState.Result -> {
                        finishWithResult(state.response)
                    }

                    is AutofillFillViewModel.UiState.Error -> {
                        // 显示错误 Toast 或直接取消
                        finishWithResult(null)
                    }
                }
            }
        }

        // 启动处理
        viewModel.initialize(request, biometricLauncher)
    }

    private fun parseIntent(intent: Intent?): AutofillFillViewModel.FillRequest {
        val raw = intent?.getStringExtra("autofill_ui_mode")
        val uiMode = when (raw) {
            "inline" -> AutofillUiMode.SYSTEM_INLINE
            "bottom_sheet" -> AutofillUiMode.BOTTOM_SHEET
            else -> AutofillUiMode.SYSTEM_INLINE
        }

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
        val directEntryId = intent?.getIntExtra("vault_item_id", -1)?.takeIf { it > 0 }
        val candidateEntryIds = intent?.getIntArrayExtra("vault_item_ids")?.toList().orEmpty()

        return AutofillFillViewModel.FillRequest(
            uiMode = uiMode,
            isUnlockOnly = isUnlockOnly,
            usernameId = usernameId,
            passwordId = passwordId,
            otpId = otpId,
            packageName = packageName,
            webDomain = webDomain,
            directEntryId = directEntryId,
            candidateEntryIds = candidateEntryIds
        )
    }

    private fun showBottomSheet(candidates: List<ResolvedCandidate>) {
        setContent {
            AppTheme {
                AuthenticationHost(this, authenticationHostRegistry) {
                    AutofillCandidateBottomSheet(
                        candidates = candidates,
                        onCandidateSelected = { candidate ->
                            viewModel.selectCandidate(candidate, biometricLauncher)
                        },
                        onCancel = { finishWithResult(null) }
                    )
                }
            }
        }
    }

    private fun finishWithResult(response: FillResponse?) {
        if (response != null) {
            setResult(RESULT_OK, Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
            })
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }
}
