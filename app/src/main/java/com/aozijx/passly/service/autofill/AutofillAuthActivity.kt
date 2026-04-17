package com.aozijx.passly.service.autofill

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.aozijx.passly.R
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.crypto.BiometricHelper
import com.aozijx.passly.core.crypto.CryptoAccess
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.security.DatabasePassphraseManager
import com.aozijx.passly.core.security.otp.TwoFAUtils
import com.aozijx.passly.core.theme.AppTheme
import com.aozijx.passly.domain.model.AutofillMatchType
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.service.autofill.presentation.AutofillRemoteViewFactory
import kotlinx.coroutines.launch

/**
 * 自动填充验证 Activity
 */
class AutofillAuthActivity : FragmentActivity() {
    private companion object {
        const val TAG = "AutofillAuthActivity"
    }

    private var selectionInProgress = false
    private val autofillRepository = AppContainer.domain.autofillUseCases

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
            performUnlock(
                usernameId,
                passwordId,
                otpId,
                pkg,
                domain
            )
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
                            onCandidateSelected = { selected: VaultEntry ->
                                if (!selectionInProgress) {
                                    selectionInProgress = true
                                    authenticateAndFill(
                                        entry = selected,
                                        usernameId = usernameId,
                                        passwordId = passwordId,
                                        otpId = otpId,
                                        uiMode = uiMode
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
                otpId = otpId,
                uiMode = uiMode
            )
        }
    }

    private fun performUnlock(
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        otpId: AutofillId?,
        packageName: String?,
        webDomain: String?
    ) {
        val cipher = DatabasePassphraseManager.getInitializedCipher(this)
        BiometricHelper.authenticate(
            activity = this,
            title = getString(R.string.vault_auth_decrypt_title),
            subtitle = getString(R.string.vault_auth_decrypt_subtitle_generic),
            cryptoObject = cipher?.let { androidx.biometric.BiometricPrompt.CryptoObject(it) },
            onSuccess = { result ->
                val passphrase = DatabasePassphraseManager.processResult(this, result)
                DatabasePassphraseManager.setDecryptedPassphrase(passphrase)

                // 解锁成功后，查询候选并返回 FillResponse
                lifecycleScope.launch {
                    val candidates =
                        autofillRepository.findMatchingCandidates(packageName, webDomain)
                    if (candidates.isEmpty()) {
                        setResult(RESULT_OK) // 成功解锁但无匹配项
                        finish()
                        return@launch
                    }

                    val responseBuilder = FillResponse.Builder()
                    candidates.forEach { candidate ->
                        val entry = candidate.entry
                        val decryptedUsername =
                            (CryptoAccess.decryptOrNull(entry.username) ?: "").trim()
                        val subtitle =
                            AutofillCredentialProvider.buildSubtitle(entry, decryptedUsername)
                        val badge = when (candidate.matchType) {
                            AutofillMatchType.APP -> getString(R.string.autofill_match_app)
                            AutofillMatchType.DOMAIN -> getString(R.string.autofill_match_domain)
                            AutofillMatchType.UNKNOWN -> getString(R.string.autofill_match_unknown)
                        }

                        val presentation = AutofillRemoteViewFactory.createDatasetItem(
                            context = applicationContext,
                            entry = entry,
                            subtitle = subtitle,
                            badge = badge
                        )

                        // 这种情况下，数据集也需要 auth，或者我们可以直接填入？
                        // 为了安全，建议解锁后点击具体项仍需二次验证（硬件解密流程要求），
                        // 或者这里直接生成带值的 Dataset（因为刚刚已经验证过了）。
                        // 考虑到用户体验，刚才已经指纹解锁了，这里可以直接返回带值的 Dataset。

                        val basicCred = AutofillCredentialProvider.getBasicCredentials(entry)
                        if (basicCred != null) {
                            val totpCode =
                                if (otpId != null && entry.totpSecret?.isNotBlank() == true) {
                                    TwoFAUtils.generateCurrentTotpFromEntry(entry)
                                } else null

                            val dataset = buildDataset(
                                usernameId = usernameId,
                                passwordId = passwordId,
                                otpId = otpId,
                                username = basicCred.username,
                                password = basicCred.password,
                                totpCode = totpCode,
                                presentation = presentation
                            )
                            if (dataset != null) {
                                responseBuilder.addDataset(dataset)
                            }
                        }
                    }

                    val resultIntent = Intent().apply {
                        putExtra(
                            AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                            responseBuilder.build()
                        )
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            },
            onError = {
                setResult(RESULT_CANCELED)
                finish()
            }
        )
    }

    private fun authenticateAndFill(
        entry: VaultEntry,
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        otpId: AutofillId?,
        uiMode: AutofillUiMode
    ) {
        Logcat.d(
            TAG,
            "authenticateAndFill: entryId=${entry.id}, usernameId=${usernameId != null}, passwordId=${passwordId != null}, otpId=${otpId != null}, uiMode=$uiMode"
        )
        // 注意：这里需要传入 CryptoObject 以支持硬件解密
        val cipher = DatabasePassphraseManager.getInitializedCipher(this)
        BiometricHelper.authenticate(
            activity = this,
            title = getString(R.string.autofill_auth_title),
            subtitle = getString(R.string.autofill_auth_subtitle),
            cryptoObject = cipher?.let { androidx.biometric.BiometricPrompt.CryptoObject(it) },
            onSuccess = { result ->
                // 确保解密口令已就绪（如果刚才还没就绪的话）
                if (DatabasePassphraseManager.isLocked) {
                    val passphrase = DatabasePassphraseManager.processResult(this, result)
                    DatabasePassphraseManager.setDecryptedPassphrase(passphrase)
                }

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
                    val resultIntent = Intent()
                    if (uiMode == AutofillUiMode.BOTTOM_SHEET) {
                        resultIntent.putExtra(
                            AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                            FillResponse.Builder().addDataset(dataset).build()
                        )
                    } else {
                        resultIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
                    }
                    setResult(RESULT_OK, resultIntent)
                    Logcat.i(TAG, "Autofill result built successfully (uiMode=$uiMode)")

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
        totpCode: String?,
        presentation: android.widget.RemoteViews? = null
    ): Dataset? {
        val builder = Dataset.Builder()
        var added = false

        fun addField(id: AutofillId?, text: String?) {
            if (id == null || text.isNullOrBlank()) return
            val value = AutofillValue.forText(text)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val fieldBuilder = Field.Builder().setValue(value)
                if (presentation != null) {
                    fieldBuilder.setPresentations(
                        android.service.autofill.Presentations.Builder()
                            .setMenuPresentation(presentation)
                            .build()
                    )
                }
                builder.setField(id, fieldBuilder.build())
            } else {
                @Suppress("DEPRECATION")
                if (presentation != null) {
                    builder.setValue(id, value, presentation)
                } else {
                    builder.setValue(id, value)
                }
            }
            added = true
        }

        addField(usernameId, username)
        addField(passwordId, password)
        addField(otpId, totpCode)

        return if (added) builder.build() else null
    }
}