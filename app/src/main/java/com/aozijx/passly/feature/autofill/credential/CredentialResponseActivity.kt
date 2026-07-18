package com.aozijx.passly.feature.autofill.credential

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.service.autofill.credential.ModernCredentialService
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import com.aozijx.passly.ui.authentication.AuthenticationHost
import com.aozijx.passly.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@AndroidEntryPoint
class CredentialResponseActivity : AppCompatActivity() {

    @Inject
    lateinit var authenticationHostRegistry: AuthenticationHostRegistry

    private val viewModel: CredentialResponseViewModel by lazy {
        ViewModelProvider(this)[CredentialResponseViewModel::class.java]
    }

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
                    is CredentialResponseViewModel.UiState.Success -> {
                        setResult(RESULT_OK, state.resultIntent)
                        finish()
                    }

                    is CredentialResponseViewModel.UiState.Error -> finishWithError()
                    is CredentialResponseViewModel.UiState.Loading -> { /* 等待结果 */
                    }
                }
            }
        }

        when (val action = intent.action) {
            ModernCredentialService.ACTION_GET_PASSWORD -> {
                val credentialData =
                    intent.getBundleExtra(ModernCredentialService.EXTRA_CREDENTIAL_DATA)
                if (credentialData == null) {
                    Logcat.e(TAG, "Missing credential data in intent")
                    finishWithError()
                    return
                }
                viewModel.handlePasswordGet(credentialData)
            }

            ModernCredentialService.ACTION_GET_PASSKEY -> {
                viewModel.handlePasskeyGet(intent)
            }

            else -> {
                Logcat.w(TAG, "Unknown action: $action")
                finishWithError()
            }
        }
    }

    private fun finishWithError() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
