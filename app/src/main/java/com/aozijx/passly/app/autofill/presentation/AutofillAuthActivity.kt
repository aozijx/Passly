package com.aozijx.passly.app.autofill.presentation

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.feature.autofill.platform.AutofillHostLaunchContract
import com.aozijx.passly.feature.autofill.presentation.AutofillFeatureEntry
import com.aozijx.passly.feature.autofill.presentation.AutofillHostCompletion
import com.aozijx.passly.presentation.shared.components.VaultItemIcon
import com.aozijx.passly.presentation.shared.entryvisual.EntryClassificationInput
import com.aozijx.passly.presentation.shared.entryvisual.EntryVisualCategoryClassifier
import com.aozijx.passly.presentation.shared.entryvisual.vaultItemIconUiModel
import com.aozijx.passly.security.authentication.host.AuthenticationHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** App-owned platform host that composes Security authentication and the Autofill Feature entry. */
@AndroidEntryPoint
class AutofillAuthActivity : AppCompatActivity() {
    @Inject lateinit var activeTaskClearRequests: ActiveTaskClearRequests

    @Inject lateinit var noticePublisher: AppNoticePublisher

    private lateinit var taskClearActivityOwner: TaskClearActivityOwner

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(themeFor(intent.action == AutofillHostLaunchContract.ACTION_LEGACY_FILL))
        super.onCreate(savedInstanceState)
        taskClearActivityOwner =
            TaskClearActivityOwner(
                requests = activeTaskClearRequests,
                scheduler = ViewTaskClearCallbackScheduler(window.decorView),
                showReminder = { noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER)) },
                closeTask = ::finishAndRemoveTask,
            )
        excludeFromAutofill()
        if (intent.action == AutofillHostLaunchContract.ACTION_ACCESSIBILITY_FILL) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        setContent {
            PersistedAppTheme {
                AuthenticationHost(this) {
                    AutofillFeatureEntry(
                        sourceIntent = intent,
                        entryIcon = { entry, modifier -> AutofillEntryIcon(entry, modifier) },
                        onComplete = ::completeAutofill,
                    )
                }
            }
        }
    }

    private fun themeFor(isLegacyFill: Boolean): Int =
        if (isLegacyFill) {
            R.style.Theme_Passly_AutofillFill
        } else {
            R.style.Theme_Passly_TransparentAuth
        }

    private fun completeAutofill(completion: AutofillHostCompletion) {
        when (completion) {
            is AutofillHostCompletion.ActivityResult -> {
                setResult(completion.resultCode, completion.data)
                hideIme()
            }

            AutofillHostCompletion.AccessibilityFinished -> {
                Unit
            }
        }
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

@Composable
private fun AutofillEntryIcon(
    entry: EntryListItem,
    modifier: Modifier,
) {
    val domain = entry.associatedDomain?.takeIf(String::isNotBlank)
    val appPackage = entry.associatedAppPackage?.takeIf(String::isNotBlank)
    VaultItemIcon(
        modifier = modifier.size(40.dp),
        model =
            vaultItemIconUiModel(
                iconName = entry.icon.name,
                customImagePath = entry.iconCustomPath,
                iconColor = entry.icon.color,
                associatedAppPackage = null,
                visualCategory =
                    EntryVisualCategoryClassifier.classify(
                        EntryClassificationInput(
                            entryType = entry.entryType,
                            title = entry.title,
                            username = entry.username,
                            urls = setOfNotNull(domain),
                            domains = setOfNotNull(domain),
                            packageNames = setOfNotNull(appPackage),
                            appNames = setOfNotNull(entry.title.takeIf { appPackage != null && domain == null }),
                        ),
                    ),
            ),
    )
}
