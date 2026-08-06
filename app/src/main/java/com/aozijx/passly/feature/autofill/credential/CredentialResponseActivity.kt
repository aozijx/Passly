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
import com.aozijx.passly.core.ui.theme.AppTheme
import com.aozijx.passly.feature.auth.ui.host.AuthenticationHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import com.aozijx.passly.service.autofill.credential.ModernCredentialService
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
                        if (!resultFinishing.compareAndSet(false, true)) {
                            return@collectLatest
                        }
                        // PendingIntentHandler requires RESULT_OK for both valid
                        // responses and valid Credential Manager exceptions.
                        setResult(RESULT_OK, state.resultIntent)
                        hideIme()
                        viewModel.closeRequestSession()
                        finish()
                    }

                    is CredentialResponseUiState.Unrecoverable -> finishWithError()
                    is CredentialResponseUiState.Loading -> { /* 等待结果 */
                    }
                }
            }
        }

        when (val action = intent.action) {
            ModernCredentialService.ACTION_GET_PASSWORD -> viewModel.handlePasswordGet(intent)

            ModernCredentialService.ACTION_UNLOCK -> viewModel.handleUnlock(intent)

            ModernCredentialService.ACTION_CREATE_PASSWORD ->
                viewModel.handlePasswordCreate(intent)

            else -> {
                AppTelemetry.w(TAG, "Unknown action: $action")
                viewModel.rejectUnknownAction()
            }
        }
    }

    private fun finishWithError() {
        setResult(RESULT_CANCELED)
        hideIme()
        finish()
    }

    private fun hideIme() {
        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())
    }
}
