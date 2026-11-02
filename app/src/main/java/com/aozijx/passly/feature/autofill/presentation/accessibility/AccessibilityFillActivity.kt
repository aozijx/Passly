package com.aozijx.passly.feature.autofill.presentation.accessibility

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.ActiveTaskClearRequests
import com.aozijx.passly.app.TaskClearActivityOwner
import com.aozijx.passly.app.ViewTaskClearCallbackScheduler
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.theme.PersistedAppTheme
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.feature.autofill.presentation.legacy.ui.AutofillCandidateBottomSheet
import com.aozijx.passly.feature.autofill.presentation.legacy.ui.toUiItem
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccessibilityFillActivity : FragmentActivity() {
    @Inject lateinit var authenticationHostRegistry: AuthenticationHostRegistry

    @Inject lateinit var logger: Logger

    @Inject lateinit var activeTaskClearRequests: ActiveTaskClearRequests

    @Inject lateinit var noticePublisher: AppNoticePublisher

    private val viewModel: AccessibilityFillViewModel by viewModels()
    private lateinit var taskClearActivityOwner: TaskClearActivityOwner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskClearActivityOwner =
            TaskClearActivityOwner(
                requests = activeTaskClearRequests,
                scheduler = ViewTaskClearCallbackScheduler(window.decorView),
                showReminder = { noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER)) },
                closeTask = ::finishAndRemoveTask,
            )
        excludeFromAutofill()
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
                        -> {
                            Unit
                        }

                        is AccessibilityFillUiState.Candidates -> {
                            AutofillCandidateBottomSheet(
                                candidates = current.values.map { it.toUiItem() },
                                onCandidateSelected = viewModel::select,
                                onCancel = viewModel::cancel,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) viewModel.cancel()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        taskClearActivityOwner.onResumed()
    }

    override fun onPause() {
        taskClearActivityOwner.onPaused()
        super.onPause()
    }
}
