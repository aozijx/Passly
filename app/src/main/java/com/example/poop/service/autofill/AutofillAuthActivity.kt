package com.example.poop.service.autofill

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.Toast
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.poop.R
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.utils.BiometricHelper
import com.example.poop.ui.screens.vault.utils.CryptoManager
import com.example.poop.ui.screens.vault.utils.TwoFAUtils
import com.example.poop.util.Logcat
import kotlinx.coroutines.launch

/**
 * 自动填充验证 Activity
 */
class AutofillAuthActivity : FragmentActivity() {
    private companion object {
        const val TAG = "AutofillAuthActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val entry = IntentCompat.getSerializableExtra(intent, "vault_item", VaultEntry::class.java)
        val usernameId = IntentCompat.getParcelableExtra(intent, "username_id", AutofillId::class.java)
        val passwordId = IntentCompat.getParcelableExtra(intent, "password_id", AutofillId::class.java)
        val otpId = IntentCompat.getParcelableExtra(intent, "otp_id", AutofillId::class.java)

        if (entry == null) {
            Logcat.e(TAG, "Entry is null")
            finish()
            return
        }

        BiometricHelper.authenticate(
            activity = this,
            title = getString(R.string.autofill_auth_title),
            subtitle = getString(R.string.autofill_auth_subtitle),
            onSuccess = {
                // 调用 getBasicCredentials 触发自动回退解密机制 (Silent -> Secure)
                val basicCred = AutofillCredentialProvider.getBasicCredentials(entry)
                if (basicCred == null) {
                    Logcat.e(TAG, "Failed to decrypt credentials")
                    Toast.makeText(this, "解密失败", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                    finish()
                    return@authenticate
                }

                // TOTP 解密同样不再需要显式指定 isSilent，内部已支持回退
                val totpCode = if (otpId != null && entry.totpSecret?.isNotBlank() == true) {
                    TwoFAUtils.generateCurrentTotpFromEntry(entry, CryptoManager)
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
                    // 修复：Dataset authentication 必须返回 Dataset 而不是 FillResponse
                    setResult(RESULT_OK, Intent().apply {
                        putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
                    })

                    lifecycleScope.launch {
                        AutofillRepository.updateUsageStats(applicationContext, entry)
                    }
                } else {
                    setResult(RESULT_CANCELED)
                }
                finish()
            },
            onError = { error ->
                Logcat.e(TAG, "Auth failed: $error")
                setResult(RESULT_CANCELED)
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
