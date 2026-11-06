package com.aozijx.passly.app.autofill.presentation

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.aozijx.passly.R
import com.aozijx.passly.app.ActiveTaskClearRequests
import com.aozijx.passly.app.TaskClearActivityOwner
import com.aozijx.passly.app.ViewTaskClearCallbackScheduler
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.theme.PersistedAppTheme
import com.aozijx.passly.feature.autofill.presentation.AutofillFeatureEntry
import com.aozijx.passly.feature.autofill.presentation.AutofillHostCompletion
import com.aozijx.passly.feature.autofill.presentation.AutofillHostPresentation
import com.aozijx.passly.feature.autofill.presentation.autofillHostPresentation
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** App-owned theme and authentication host around the Autofill Feature entry point. */
@AndroidEntryPoint
class AutofillAuthActivity : AppCompatActivity() {
    @Inject lateinit var activeTaskClearRequests: ActiveTaskClearRequests
    @Inject lateinit var noticePublisher: AppNoticePublisher

    private lateinit var taskClearActivityOwner: TaskClearActivityOwner

    override fun onCreate(savedInstanceState: Bundle?) {
        val presentation = autofillHostPresentation(intent.action)
        setTheme(themeFor(presentation))
        super.onCreate(savedInstanceState)
        taskClearActivityOwner =
            TaskClearActivityOwner(
                requests = activeTaskClearRequests,
                scheduler = ViewTaskClearCallbackScheduler(window.decorView),
                showReminder = { noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER)) },
                closeTask = ::finishAndRemoveTask,
            )
        excludeFromAutofill()
        if (presentation == AutofillHostPresentation.ACCESSIBILITY_FILL) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        setContent {
            PersistedAppTheme {
                AuthenticationHost(this) {
                    AutofillFeatureEntry(
                        sourceIntent = intent,
                        onComplete = ::completeAutofill,
                    )
                }
            }
        }
    }

    private fun themeFor(presentation: AutofillHostPresentation): Int =
        if (presentation == AutofillHostPresentation.LEGACY_FILL) {
            R.style.Theme_Passly_AutofillFill
        } else {
            R.style.Theme_Passly_TransparentAuth
        }

    private fun completeAutofill(completion: AutofillHostCompletion) {
        completion.resultCode?.let { setResult(it, completion.data) }
        if (completion.hideIme) hideIme()
        finish()
    }

    private fun hideIme() {
        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
    }

    override fun onResume() {
        super.onResume()
        taskClearActivityOwner.onResumed()
    }

    override fun onPause() {
        taskClearActivityOwner.onPaused()
        super.onPause()
    }

    @Suppress("DEPRECATION")
    private fun excludeFromAutofill() {
        window.decorView.importantForAutofill =
            android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
}
