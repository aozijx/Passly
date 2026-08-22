package com.aozijx.passly.feature.autofill.credential

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.ui.components.auth.AuthenticationHost
import com.aozijx.passly.core.ui.theme.AppTheme
import com.aozijx.passly.feature.autofill.credential.service.ModernCredentialService
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@AndroidEntryPoint
class CredentialResponseActivity : AppCompatActivity() {

    @Inject
    lateinit var authenticationHostRegistry: AuthenticationHostRegistry

    private val viewModel: CredentialResponseViewModel by viewModels()
    private val resultFinishing = AtomicBoolean(false)

    /** 是否为最终步骤（get/create）：仅最终步骤完成后关闭自动填充会话。 */
    private var isFinalStepAction = false

    companion object {
        private const val TAG = "CredResponse"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                AuthenticationHost(this, authenticationHostRegistry) {}
            }
        }

        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is CredentialResponseUiState.Complete -> {
                        if (!resultFinishing.compareAndSet(false, true)) return@collectLatest
                        // PendingIntentHandler requires RESULT_OK for both valid
                        // responses and valid Credential Manager exceptions.
                        setResult(RESULT_OK, state.resultIntent)
                        hideIme()
                        // 解锁动作（ACTION_UNLOCK）是中间步骤：用户随后选择条目会再进
                        // 一个 ACTION_GET_PASSWORD Activity，此刻关闭会话会把 vault
                        // SEAL（擦 DEK），导致第二次认证。仅最终步骤（get/create）关闭。
                        if (isFinalStepAction) viewModel.closeRequestSession()
                        finish()
                    }

                    is CredentialResponseUiState.Unrecoverable -> finishWithError()
                    is CredentialResponseUiState.Loading -> { /* 等待结果 */
                    }
                }
            }
        }

        when (val action = intent.action) {
            ModernCredentialService.ACTION_GET_PASSWORD -> {
                isFinalStepAction = true
                viewModel.onAction(CredentialResponseUiAction.PasswordGet(intent))
            }

            ModernCredentialService.ACTION_UNLOCK ->
                viewModel.onAction(CredentialResponseUiAction.Unlock(intent))

            ModernCredentialService.ACTION_CREATE_PASSWORD -> {
                isFinalStepAction = true
                viewModel.onAction(CredentialResponseUiAction.PasswordCreate(intent))
            }

            else -> {
                AppTelemetry.w(TAG, "Unknown action: $action")
                viewModel.onAction(CredentialResponseUiAction.UnknownAction)
            }
        }
    }

    private fun finishWithError() {
        setResult(RESULT_CANCELED)
        hideIme()
        lifecycleScope.launch { viewModel.closeRequestSession() }
        finish()
    }

    override fun onDestroy() {
        // 返回键/系统回收路径没有走 Complete/Error（resultFinishing=false），
        // 兜底回收会话，避免临时解锁泄漏。配置变更销毁（isFinishing=false）不回收。
        if (!resultFinishing.get() && isFinishing) {
            lifecycleScope.launch { viewModel.closeRequestSession() }
        }
        super.onDestroy()
    }

    private fun hideIme() {
        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())
    }
}
