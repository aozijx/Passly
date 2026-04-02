package com.aozijx.passly.service.autofill

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.CustomDescription
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.Presentations
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.widget.RemoteViews
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.util.AutofillParser
import com.aozijx.passly.core.util.Logcat
import com.aozijx.passly.core.util.PackageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Poop 自动填充服务
 */
class AutofillService : android.service.autofill.AutofillService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "PoopAutofill"

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure

        serviceScope.launch {
            try {
                val parser = AutofillParser(structure)
                Logcat.d(tag, "onFillRequest: pkg=${parser.packageName}, domain=${parser.webDomain}")

                val availableIds = listOfNotNull(parser.usernameId, parser.passwordId, parser.otpId)
                val entries = AutofillRepository.findMatchingEntries(applicationContext, parser.packageName, parser.webDomain)
                val responseBuilder = FillResponse.Builder()

                entries.forEach { entry ->
                    // 核心修复：显式指定类型避免类型推断错误
                    val decryptedUsername = try { CryptoManager.decrypt(entry.username) } catch (_: Exception) { "" }.ifBlank { "点击填充" }

                    val presentation = RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                        setTextViewText(R.id.title, entry.title)
                        setTextViewText(R.id.username, if (!entry.totpSecret.isNullOrBlank()) "OTP: $decryptedUsername" else decryptedUsername)
                        entry.associatedAppPackage?.let { appPkg ->
                            PackageUtils.getAppIconDrawable(applicationContext, appPkg)?.let { icon ->
                                setImageViewBitmap(R.id.icon, PackageUtils.drawableToBitmap(icon))
                            }
                        }
                    }

                    val authIntent = Intent(this@AutofillService, AutofillAuthActivity::class.java).apply {
                        putExtra("vault_item", entry)
                        putExtra("username_id", parser.usernameId)
                        putExtra("password_id", parser.passwordId)
                        putExtra("otp_id", parser.otpId)
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        this@AutofillService,
                        entry.id.hashCode() xor System.nanoTime().toInt(),
                        authIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val datasetBuilder = Dataset.Builder().setAuthentication(pendingIntent.intentSender)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val field = Field.Builder().setPresentations(Presentations.Builder().setMenuPresentation(presentation).build()).build()
                        availableIds.forEach { datasetBuilder.setField(it, field) }
                    } else {
                        @Suppress("DEPRECATION")
                        availableIds.forEach { id ->
                            datasetBuilder.setValue(id, null, presentation)
                        }
                    }

                    responseBuilder.addDataset(datasetBuilder.build())
                }

                val saveIds = listOfNotNull(parser.usernameId, parser.passwordId)
                if (saveIds.isNotEmpty()) {
                    val saveInfoBuilder = SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME,
                        saveIds.toTypedArray()
                    )

                    val appData = parser.packageName?.let { pkg ->
                        PackageUtils.getAppLabelAndIcon(applicationContext, pkg)
                    }
                    val appLabel = appData?.first ?: parser.webDomain ?: "该应用"
                    val iconBitmap = appData?.second?.let { PackageUtils.drawableToBitmap(it) }

                    if (iconBitmap != null) {
                        val customDescription = CustomDescription.Builder(
                            RemoteViews(packageName, R.layout.autofill_save_description).apply {
                                setImageViewBitmap(R.id.save_icon, iconBitmap)
                                setTextViewText(R.id.save_title, "保存 $appLabel ？")
                                setTextViewText(R.id.save_description, "是否保存 $appLabel 的账号密码？")
                            }
                        ).build()
                        saveInfoBuilder.setCustomDescription(customDescription)
                    } else {
                        saveInfoBuilder.setDescription("是否将 $appLabel 的账号密码保存到 Poop？")
                    }
                    var saveFlags = SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE
                    if (parser.usernameId != null && parser.passwordId == null) {
                        saveFlags = saveFlags or SaveInfo.FLAG_DELAY_SAVE
                    }
                    saveInfoBuilder.setFlags(saveFlags)
                    parser.submitId?.let { saveInfoBuilder.setTriggerId(it) }

                    responseBuilder.setSaveInfo(saveInfoBuilder.build())
                }

                val response = responseBuilder.build()
                callback.onSuccess(if (entries.isNotEmpty() || saveIds.isNotEmpty()) response else null)

            } catch (e: Exception) {
                Logcat.e(tag, "Fill request failed", e)
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        var username = ""; var password = ""; var pkg: String? = null; var domain: String? = null; var title: String? = null

        // 核心修复：遍历所有 context 收集完整数据，并使用最新填入的值
        request.fillContexts.forEach { context ->
            val p = AutofillParser(context.structure)

            // 抓取包名和域名
            if (pkg == null) pkg = p.packageName
            if (domain == null) domain = p.webDomain
            if (title == null) title = p.pageTitle

            // 抓取用户输入的值
            if (!p.usernameValue.isNullOrBlank()) username = p.usernameValue!!
            if (!p.passwordValue.isNullOrBlank()) password = p.passwordValue!!
        }

        Logcat.d(tag, "onSaveRequest: captured user=$username, hasPwd=${password.isNotBlank()}, pkg=$pkg")

        if (password.isBlank()) {
            Logcat.w(tag, "onSaveRequest: password is blank, ignore save")
            return callback.onSuccess()
        }

        serviceScope.launch {
            try {
                val success = AutofillRepository.saveOrUpdateEntry(applicationContext, pkg, domain, title, username, password)
                if (success) {
                    Logcat.i(tag, "Successfully saved credentials for $username")
                    callback.onSuccess()
                } else {
                    Logcat.e(tag, "Failed to save credentials")
                    callback.onFailure("Save failed in repository")
                }
            } catch (e: Exception) {
                Logcat.e(tag, "Exception during save", e)
                callback.onFailure(e.message)
            }
        }
    }
}
