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
import com.aozijx.passly.core.telemetry.TelemetryRuntime
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.presentation.feature.shell.theme.AppTheme
import com.aozijx.passly.feature.autofill.credential.service.ModernCredentialService
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
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
            )

            ModernCredentialService.ACTION_UNLOCK -> CredentialResponseLaunch(
                action = CredentialResponseUiAction.Unlock(sourceIntent),
            )

            ModernCredentialService.ACTION_CREATE_PASSWORD -> CredentialResponseLaunch(
                action = CredentialResponseUiAction.PasswordCreate(sourceIntent),
            )

            else -> {
                TelemetryRuntime.w(EventCategory.AUTOFILL, "credential.response_action_unknown")
                CredentialResponseLaunch(
                    action = CredentialResponseUiAction.UnknownAction,
                )
            }
        }

    private fun finishWithResult(
        resultIntent: Intent,
    ) {
        if (!resultFinishing.compareAndSet(false, true)) return
        // PendingIntentHandler requires RESULT_OK for both valid responses and
        // valid Credential Manager exceptions.
        setResult(RESULT_OK, resultIntent)
        hideIme()
        finish()
    }

    private fun finishWithError() {
        if (!resultFinishing.compareAndSet(false, true)) return
        setResult(RESULT_CANCELED)
        hideIme()
        viewModel.cancelActiveInteraction()
        finish()
    }

    override fun onDestroy() {
        // Back/system-finish paths did not deliver a response. Configuration
        // changes keep the same ViewModel and must not cancel the interaction.
        if (!resultFinishing.get() && isFinishing) {
            viewModel.cancelActiveInteraction()
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
    )
}
