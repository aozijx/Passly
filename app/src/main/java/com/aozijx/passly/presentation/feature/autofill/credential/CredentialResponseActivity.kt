package com.aozijx.passly.presentation.feature.autofill.credential

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
import com.aozijx.passly.core.telemetry.TelemetryRuntime
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.presentation.feature.shell.theme.AppTheme
import com.aozijx.passly.feature.autofill.credential.service.ModernCredentialService
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
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

    companion object {
        private const val TAG = "CredResponse"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launch = responseLaunch(intent)

        setContent {
            AppTheme {
                AuthenticationHost(this, authenticationHostRegistry) {
                    CredentialResponseRoute(
                        action = launch.action,
                        viewModel = viewModel,
                        onComplete = { resultIntent ->
                            finishWithResult(
                                resultIntent = resultIntent,
                                closeSession = launch.closeSessionOnComplete,
                            )
                        },
                        onUnrecoverable = ::finishWithError,
                    )
                }
            }
        }
    }

    private fun responseLaunch(sourceIntent: Intent): CredentialResponseLaunch =
        when (val action = sourceIntent.action) {
            ModernCredentialService.ACTION_GET_PASSWORD -> CredentialResponseLaunch(
                action = CredentialResponseUiAction.PasswordGet(sourceIntent),
                closeSessionOnComplete = true,
            )

            ModernCredentialService.ACTION_UNLOCK -> CredentialResponseLaunch(
                action = CredentialResponseUiAction.Unlock(sourceIntent),
                closeSessionOnComplete = false,
            )

            ModernCredentialService.ACTION_CREATE_PASSWORD -> CredentialResponseLaunch(
                action = CredentialResponseUiAction.PasswordCreate(sourceIntent),
                closeSessionOnComplete = true,
            )

            else -> {
                TelemetryRuntime.w(EventCategory.AUTOFILL, "credential.response_action_unknown")
                CredentialResponseLaunch(
                    action = CredentialResponseUiAction.UnknownAction,
                    closeSessionOnComplete = true,
                )
            }
        }

    private fun finishWithResult(
        resultIntent: Intent,
        closeSession: Boolean,
    ) {
        if (!resultFinishing.compareAndSet(false, true)) return
        // PendingIntentHandler requires RESULT_OK for both valid responses and
        // valid Credential Manager exceptions.
        setResult(RESULT_OK, resultIntent)
        hideIme()
        // Unlock is an intermediate step. Closing its request session would seal
        // the vault before the subsequent password request and force authentication again.
        lifecycleScope.launch {
            if (closeSession) viewModel.closeRequestSession()
            finish()
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

    private data class CredentialResponseLaunch(
        val action: CredentialResponseUiAction,
        val closeSessionOnComplete: Boolean,
    )
}
