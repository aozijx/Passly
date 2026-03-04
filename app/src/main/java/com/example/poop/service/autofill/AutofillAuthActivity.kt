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
import com.example.poop.data.VaultItem
import com.example.poop.ui.screens.vault.utils.BiometricHelper
import com.example.poop.util.Logcat
import kotlinx.coroutines.launch

/**
 * 自动填充验证 Activity (重构版：高内聚低耦合)
 * 职责：仅作为验证网关，驱动生物识别并组装结果
 */
class AutofillAuthActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始设为取消状态
        setResult(RESULT_CANCELED)

        val item = IntentCompat.getSerializableExtra(intent, "vault_item", VaultItem::class.java)
        val usernameId = IntentCompat.getParcelableExtra(intent, "username_id", AutofillId::class.java)
        val passwordId = IntentCompat.getParcelableExtra(intent, "password_id", AutofillId::class.java)
        val otpId = IntentCompat.getParcelableExtra(intent, "otp_id", AutofillId::class.java)

        if (item == null) {
            finish()
            return
        }

        // 调起生物识别
        BiometricHelper.authenticate(
            activity = this,
            title = getString(R.string.autofill_auth_title),
            subtitle = getString(R.string.autofill_auth_subtitle),
            onSuccess = {
                // 调用高内聚的业务提供者获取解密凭据
                val credentials = AutofillCredentialProvider.getCredentials(item)

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
                        AutofillRepository.updateUsageStats(applicationContext, item)
                    }
                } else {
                    // 优化：解密失败提供友好 Toast 提示
                    Toast.makeText(this, "解密凭据失败，请重试", Toast.LENGTH_SHORT).show()
                    Logcat.e("AutofillAuth", "Decryption failed during auth flow for item ${item.id}")
                    setResult(RESULT_CANCELED)
                }
                finish()
            },
            onError = {
                // 验证失败提示并返回取消状态
                Toast.makeText(this, "验证失败", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        )
    }
}