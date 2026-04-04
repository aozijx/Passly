package com.aozijx.passly.service.autofill

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.R
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.crypto.BiometricHelper
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.security.otp.TwoFAUtils
import com.aozijx.passly.domain.model.VaultEntry
import kotlinx.coroutines.launch

/**
 * 自动填充验证 Activity
 */
class AutofillAuthActivity : FragmentActivity() {
    private companion object {
        const val TAG = "AutofillAuthActivity"
    }

    private var selectionInProgress = false
    private val autofillRepository = AppContainer.autofillServiceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val uiMode = AutofillUiMode.fromKey(intent?.getStringExtra("autofill_ui_mode"))
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val usernameId =
            IntentCompat.getParcelableExtra(intent, "username_id", AutofillId::class.java)
        val passwordId =
            IntentCompat.getParcelableExtra(intent, "password_id", AutofillId::class.java)
        val otpId = IntentCompat.getParcelableExtra(intent, "otp_id", AutofillId::class.java)
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
                    MaterialTheme {
                        AutofillCandidateBottomSheet(
                            entries = candidateEntries,
                            onCandidateSelected = { selected: VaultEntry ->
                                if (!selectionInProgress) {
                                    selectionInProgress = true
                                    authenticateAndFill(
                                        entry = selected,
                                        usernameId = usernameId,
                                        passwordId = passwordId,
                                        otpId = otpId
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
                ?: IntentCompat.getSerializableExtra(intent, "vault_item", VaultEntry::class.java)

            if (entry == null) {
                Logcat.e(TAG, "Entry is null")
                finish()
                return@launch
            }

            authenticateAndFill(
                entry = entry,
                usernameId = usernameId,
                passwordId = passwordId,
                otpId = otpId
            )
        }
    }

    private fun authenticateAndFill(
        entry: VaultEntry,
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        otpId: AutofillId?
    ) {
        Logcat.d(
            TAG,
            "authenticateAndFill: entryId=${entry.id}, usernameId=${usernameId != null}, passwordId=${passwordId != null}, otpId=${otpId != null}"
        )
        BiometricHelper.authenticate(
            activity = this,
            title = getString(R.string.autofill_auth_title),
            subtitle = getString(R.string.autofill_auth_subtitle),
            onSuccess = {
                // 调用 getBasicCredentials 触发解密
                val basicCred = AutofillCredentialProvider.getBasicCredentials(entry)
                if (basicCred == null) {
                    Logcat.e(TAG, "Failed to decrypt credentials")
                    Toast.makeText(this, "解密失败", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                    finish()
                    return@authenticate
                }

                val totpCode = if (otpId != null && entry.totpSecret?.isNotBlank() == true) {
                    TwoFAUtils.generateCurrentTotpFromEntry(entry)
                } else null

                val dataset = buildDataset(
                    usernameId = usernameId,
                    passwordId = passwordId,
                    otpId = otpId,
                    username = basicCred.username,
                    password = basicCred.password,
                    totpCode = totpCode
                )

                if (dataset != null) {
                    setResult(RESULT_OK, Intent().apply {
                        putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
                    })
                    Logcat.i(TAG, "Autofill dataset built successfully")

                    lifecycleScope.launch {
                        autofillRepository.updateUsageStats(entry)
                    }
                } else {
                    Logcat.w(TAG, "Autofill dataset is null, canceling fill")
                    Toast.makeText(this, "当前页面未识别到可填充字段", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                }
                finish()
            },
            onError = { error ->
                Logcat.e(TAG, "Auth failed: $error")
                setResult(RESULT_CANCELED)
                selectionInProgress = false
                finish()
            }
        )
    }

    private fun buildDataset(
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        otpId: AutofillId?,
        username: String,
        password: String,
        totpCode: String?
    ): Dataset? {
        val builder = Dataset.Builder()
        var added = false

        fun addField(id: AutofillId?, text: String?) {
            if (id == null || text.isNullOrBlank()) return
            val value = AutofillValue.forText(text)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val field = Field.Builder()
                    .setValue(value)
                    .build()
                builder.setField(id, field)
            } else {
                @Suppress("DEPRECATION")
                builder.setValue(id, value)
            }
            added = true
        }

        addField(usernameId, username)
        addField(passwordId, password)
        addField(otpId, totpCode)

        return if (added) builder.build() else null
    }
}
