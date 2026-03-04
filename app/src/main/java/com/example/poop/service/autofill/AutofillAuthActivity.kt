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
import com.example.poop.util.Logcat
import kotlinx.coroutines.launch

/**
 * 自动填充验证 Activity
 * 职责：仅作为验证网关，驱动生物识别并组装结果
 */
class AutofillAuthActivity : FragmentActivity() {

    private companion object {
        const val TAG = "AutofillAuthActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始设为取消状态
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

        // 调起生物识别
        BiometricHelper.authenticate(
            activity = this,
            title = getString(R.string.autofill_auth_title),
            subtitle = getString(R.string.autofill_auth_subtitle),
            onSuccess = {
                val credentials = AutofillCredentialProvider.getCredentials(entry)

                if (credentials != null) {
                    val replyIntent = Intent()
                    val datasetBuilder = Dataset.Builder()

                    // 构造 Dataset
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        usernameId?.let { datasetBuilder.setField(it, Field.Builder().setValue(
                            AutofillValue.forText(credentials.username)).build()) }
                        passwordId?.let { datasetBuilder.setField(it, Field.Builder().setValue(
                            AutofillValue.forText(credentials.password)).build()) }
                        otpId?.let { id ->
                            credentials.totpCode?.let { code ->
                                datasetBuilder.setField(id, Field.Builder().setValue(AutofillValue.forText(code)).build())
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        usernameId?.let { datasetBuilder.setValue(it, AutofillValue.forText(credentials.username)) }
                        @Suppress("DEPRECATION")
                        passwordId?.let { datasetBuilder.setValue(it, AutofillValue.forText(credentials.password)) }
                        @Suppress("DEPRECATION")
                        otpId?.let { id ->
                            credentials.totpCode?.let { code ->
                                datasetBuilder.setValue(id, AutofillValue.forText(code))
                            }
                        }
                    }

                    replyIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, datasetBuilder.build())
                    setResult(RESULT_OK, replyIntent)

                    // 异步更新统计信息
                    lifecycleScope.launch {
                        AutofillRepository.updateUsageStats(applicationContext, entry)
                    }
                } else {
                    Logcat.e(TAG, "Failed to decrypt credentials for entry: ${entry.id}")
                    Toast.makeText(this, "解密凭据失败，请重试", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                }
                finish()
            },
            onError = { error ->
                Logcat.e(TAG, "Biometric auth failed: $error")
                Toast.makeText(this, "验证失败: $error", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        )
    }
}
