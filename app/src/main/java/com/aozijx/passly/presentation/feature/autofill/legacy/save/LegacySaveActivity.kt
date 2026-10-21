package com.aozijx.passly.presentation.feature.autofill.legacy.save

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.presentation.feature.shell.theme.PersistedAppTheme
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class LegacySaveActivity : AppCompatActivity() {
    @Inject
    lateinit var authenticationHostRegistry: AuthenticationHostRegistry

    @Inject
    lateinit var logger: Logger

    private val viewModel: LegacySaveViewModel by viewModels()
    private val terminal = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = intent.getStringExtra(AutofillLaunchExtras.LEGACY_SAVE_TOKEN)
        setContent {
            PersistedAppTheme {
                AuthenticationHost(this, authenticationHostRegistry, logger) {
                    LegacySaveRoute(
                        token = token,
                        viewModel = viewModel,
                        onSaved = ::finishTerminal,
                        onUnrecoverable = ::finishTerminal,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (!terminal.get() && isFinishing) {
            viewModel.cancelActiveInteraction()
        }
        super.onDestroy()
    }

    private fun finishTerminal() {
        if (!terminal.compareAndSet(false, true)) return
        finish()
    }
}
