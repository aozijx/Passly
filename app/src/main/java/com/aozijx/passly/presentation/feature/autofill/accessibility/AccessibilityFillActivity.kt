package com.aozijx.passly.presentation.feature.autofill.accessibility

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.presentation.feature.autofill.legacy.ui.AutofillCandidateBottomSheet
import com.aozijx.passly.presentation.feature.autofill.legacy.ui.toUiItem
import com.aozijx.passly.presentation.feature.shell.theme.PersistedAppTheme
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class AccessibilityFillActivity : FragmentActivity() {
    @Inject lateinit var authenticationHostRegistry: AuthenticationHostRegistry
    @Inject lateinit var logger: Logger

    private val viewModel: AccessibilityFillViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { viewModel.start() }
            LaunchedEffect(state) {
                if (state == AccessibilityFillUiState.Done) finish()
            }
            PersistedAppTheme {
                AuthenticationHost(this, authenticationHostRegistry, logger) {
                    when (val current = state) {
                        AccessibilityFillUiState.Loading,
                        AccessibilityFillUiState.Done,
                        -> Unit
                        is AccessibilityFillUiState.Candidates -> AutofillCandidateBottomSheet(
                            candidates = current.values.map { it.toUiItem() },
                            onCandidateSelected = viewModel::select,
                            onCancel = viewModel::cancel,
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) runBlocking { viewModel.abandon() }
        super.onDestroy()
    }

}
