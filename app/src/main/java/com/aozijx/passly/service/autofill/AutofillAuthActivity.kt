package com.aozijx.passly.service.autofill

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.R
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.otp.TwoFAUtils
import com.aozijx.passly.core.theme.AppTheme
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.verification.internal.VerificationCoordinator
import com.aozijx.passly.service.autofill.builder.AutofillResponseBuilder
import com.aozijx.passly.service.autofill.credential.AutofillCredentialProvider
import com.aozijx.passly.service.autofill.presenter.AutofillCandidateBottomSheet
import kotlinx.coroutines.launch

class AutofillAuthActivity : FragmentActivity() {
    private companion object {
        const val TAG = "AutofillAuthActivity"
    }

    private var selectionInProgress = false
    private val autofillRepository = AppContainer.domain.autofillUseCases
    private val verificationCoordinator = VerificationCoordinator(
        scope = lifecycleScope,
        authUseCases = AppContainer.domain.authUseCases
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val uiMode = AutofillUiMode.fromKey(intent?.getStringExtra("autofill_ui_mode"))
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val isUnlockOnly = intent.getBooleanExtra("unlock_only", false)
        val usernameId =
            IntentCompat.getParcelableExtra(intent, "username_id", AutofillId::class.java)
        val passwordId =
            IntentCompat.getParcelableExtra(intent, "password_id", AutofillId::class.java)
        val otpId = IntentCompat.getParcelableExtra(intent, "otp_id", AutofillId::class.java)

        if (isUnlockOnly) {
            val pkg = intent.getStringExtra("package_name")
            val domain = intent.getStringExtra("web_domain")
            performUnlock(usernameId, passwordId, otpId, pkg, domain)
            return
        }

        val directEntryId = intent.getIntExtra("vault_item_id", -1).takeIf { it > 0 }
        val candidateEntryIds = intent.getIntArrayExtra("vault_item_ids")?.toList().orEmpty()

        if (uiMode == AutofillUiMode.BOTTOM_SHEET && candidateEntryIds.isNotEmpty() && directEntryId == null) {
            lifecycleScope.launch {
                val candidateEntries = autofillRepository.getEntriesByIds(candidateEntryIds)
                if (candidateEntries.isEmpty()) {
                    Logcat.e(TAG, "Candidate entries are empty after loading by IDs")
                    finish()
                    return@launch
                }
                setContent {
                    AppTheme {
                        AutofillCandidateBottomSheet(
                            entries = candidateEntries,
                            onCandidateSelected = { selected ->
                                if (!selectionInProgress) {
                                    selectionInProgress = true
                                    authenticateAndFill(
                                        selected,
                                        usernameId,
                                        passwordId,
                                        otpId,
                                        uiMode
                                    )
                                }
                            },
                            onCancel = { finish() }
                        )
                    }
                }
            }
            return
        }

        lifecycleScope.launch {
            val entry = directEntryId?.let { autofillRepository.getEntryById(it) }
            if (entry == null) {
                Logcat.e(TAG, "Entry is null")
                finish()
                return@launch
            }
            authenticateAndFill(entry, usernameId, passwordId, otpId, uiMode)
        }
    }

    private fun performUnlock(
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        otpId: AutofillId?,
        packageName: String?,
        webDomain: String?
    ) {
        lifecycleScope.launch {
            val authResult = verificationCoordinator.verifyWithBiometricSuspended(
                this@AutofillAuthActivity,
                getString(R.string.vault_auth_decrypt_title),
                getString(R.string.vault_auth_decrypt_subtitle_generic)
            )
            if (authResult is AppResult.Failure) {
                cancelAndFinish()
                return@launch
            }

            val candidates = autofillRepository.findMatchingCandidates(packageName, webDomain)
            if (candidates.isEmpty()) {
                finishWithOk()
                return@launch
            }
            val response = AutofillResponseBuilder.buildPostUnlockFillResponse(
                applicationContext, candidates, usernameId, passwordId, otpId
            )
            if (response != null) finishWithOk(response) else cancelAndFinish()
        }
    }

    private fun authenticateAndFill(
        entry: VaultEntry,
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        otpId: AutofillId?,
        uiMode: AutofillUiMode
    ) {
        Logcat.d(TAG, "authenticateAndFill: entryId=${entry.id}, uiMode=$uiMode")
        lifecycleScope.launch {
            val authResult = verificationCoordinator.verifyWithBiometricSuspended(
                this@AutofillAuthActivity,
                getString(R.string.autofill_auth_title),
                getString(R.string.autofill_auth_subtitle)
            )
            if (authResult is AppResult.Failure) {
                selectionInProgress = false
                cancelAndFinish()
                return@launch
            }

            val basicCred = AutofillCredentialProvider.getBasicCredentials(entry)
            if (basicCred == null) {
                Logcat.e(TAG, "Failed to decrypt credentials")
                Toast.makeText(this@AutofillAuthActivity, "解密失败", Toast.LENGTH_SHORT).show()
                cancelAndFinish()
                return@launch
            }

            val totpCode = if (otpId != null && entry.totpSecret?.isNotBlank() == true)
                TwoFAUtils.generateCurrentTotpFromEntry(entry) else null

            val dataset = AutofillResponseBuilder.createFillDataset(
                usernameId, passwordId, otpId,
                basicCred.username, basicCred.password, totpCode
            )

            if (dataset != null) {
                Logcat.i(TAG, "Autofill result built successfully (uiMode=$uiMode)")
                lifecycleScope.launch { autofillRepository.updateUsageStats(entry) }
                if (uiMode == AutofillUiMode.BOTTOM_SHEET) {
                    finishWithOk(FillResponse.Builder().addDataset(dataset).build())
                } else {
                    finishWithOk(dataset)
                }
            } else {
                Logcat.w(TAG, "Autofill dataset is null, canceling fill")
                Toast.makeText(
                    this@AutofillAuthActivity,
                    "当前页面未识别到可填充字段",
                    Toast.LENGTH_SHORT
                ).show()
                cancelAndFinish()
            }
        }
    }

    private fun finishWithOk() {
        setResult(RESULT_OK)
        finish()
    }

    private fun finishWithOk(extra: Parcelable) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, extra)
        })
        finish()
    }

    private fun cancelAndFinish() {
        setResult(RESULT_CANCELED)
        finish()
    }
}