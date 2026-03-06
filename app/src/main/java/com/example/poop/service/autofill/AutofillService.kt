package com.example.poop.service.autofill

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.Presentations
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import com.example.poop.R
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.autofill.AutofillTitleGenerator
import com.example.poop.ui.screens.vault.utils.AutofillParser
import com.example.poop.ui.screens.vault.utils.CryptoManager
import com.example.poop.util.Logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Poop 自动填充服务
 * 修复：实现覆盖保存与精准匹配，解决返回不填充问题
 */
class AutofillService : AutofillService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "PoopAutofill"
    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val parser = AutofillParser(structure)
        
        Logcat.d(tag, "onFillRequest: pkg=${parser.packageName}, domain=${parser.webDomain}, userFound=${parser.usernameId != null}, pwdFound=${parser.passwordId != null}")

        serviceScope.launch {
            try {
                // 如果一个可填充字段都没找到，直接返回成功但无数据
                if (parser.usernameId == null && parser.passwordId == null) {
                    Logcat.w(tag, "No fillable fields found in structure")
                    callback.onSuccess(null)
                    return@launch
                }

                val db = AppDatabase.getDatabase(applicationContext)
                val entries = db.vaultDao().getAllEntries().first()

                // 优先通过包名和域名精准匹配
                val filteredEntries = entries.filter { entry ->
                    val pkgMatch = parser.packageName != null && entry.associatedAppPackage == parser.packageName
                    val domainMatch = parser.webDomain != null && entry.associatedDomain == parser.webDomain
                    pkgMatch || domainMatch
                }.sortedByDescending { it.updatedAt ?: it.createdAt ?: 0L }.take(5)

                Logcat.d(tag, "Found ${filteredEntries.size} matching entries for ${parser.packageName ?: parser.webDomain}")

                val responseBuilder = FillResponse.Builder()
                var hasDatasets = false

                if (filteredEntries.isNotEmpty()) {
                    filteredEntries.forEach { entry ->
                        val presentation = RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                            setTextViewText(R.id.title, entry.title)
                            setTextViewText(R.id.username, "点击填充")
                        }

                        val authIntent = Intent(this@AutofillService, AutofillAuthActivity::class.java).apply {
                            putExtra("vault_item", entry)
                            putExtra("username_id", parser.usernameId)
                            putExtra("password_id", parser.passwordId)
                        }

                        val pendingIntent = PendingIntent.getActivity(
                            this@AutofillService, entry.id, authIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val datasetBuilder = Dataset.Builder()
                        datasetBuilder.setAuthentication(pendingIntent.intentSender)

                        var fieldAdded = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val field = Field.Builder()
                                .setPresentations(Presentations.Builder().setMenuPresentation(presentation).build())
                                .build()
                            parser.usernameId?.let { datasetBuilder.setField(it, field); fieldAdded = true }
                            parser.passwordId?.let { datasetBuilder.setField(it, field); fieldAdded = true }
                        } else {
                            @Suppress("DEPRECATION")
                            parser.usernameId?.let { datasetBuilder.setValue(it, null, presentation); fieldAdded = true }
                            @Suppress("DEPRECATION")
                            parser.passwordId?.let { datasetBuilder.setValue(it, null, presentation); fieldAdded = true }
                        }
                        
                        if (fieldAdded) {
                            responseBuilder.addDataset(datasetBuilder.build())
                            hasDatasets = true
                        }
                    }
                }

                // 始终设置 SaveInfo 监听，这样用户修改密码也能提示保存
                val saveIds = mutableListOf<AutofillId>()
                parser.usernameId?.let { saveIds.add(it) }
                parser.passwordId?.let { saveIds.add(it) }

                var hasSaveInfo = false
                if (saveIds.isNotEmpty()) {
                    val saveInfo = SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME,
                        saveIds.toTypedArray()
                    ).build()
                    responseBuilder.setSaveInfo(saveInfo)
                    hasSaveInfo = true
                }

                val response = responseBuilder.build()
                if (hasDatasets || hasSaveInfo) {
                    Logcat.d(tag, "Sending FillResponse: datasets=$hasDatasets, hasSaveInfo=$hasSaveInfo")
                    callback.onSuccess(response)
                } else {
                    Logcat.d(tag, "No datasets and no SaveInfo, returning null")
                    callback.onSuccess(null)
                }
            } catch (e: Exception) {
                Logcat.e(tag, "Fill request failed", e)
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.last().structure
        val parser = AutofillParser(structure)

        val usernameValue = parser.usernameValue ?: ""
        val passwordValue = parser.passwordValue ?: ""
        val packageName = parser.packageName
        val webDomain = parser.webDomain

        Logcat.d(tag, "onSaveRequest: pkg=$packageName, userLen=${usernameValue.length}, pwdLen=${passwordValue.length}")

        if (passwordValue.isBlank()) {
            Logcat.w(tag, "Save request ignored: password is blank")
            callback.onSuccess()
            return
        }

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val dao = db.vaultDao()

                // 查找该应用/域名下的现有条目，用于更新
                val existing = dao.getAllEntries().first().find {
                    (it.associatedAppPackage == packageName && packageName != null) ||
                    (it.associatedDomain == webDomain && webDomain != null)
                }

                val encUser = CryptoManager.encrypt(usernameValue, isSilent = true)
                val encPass = CryptoManager.encrypt(passwordValue, isSilent = true)
                
                if (encUser != null && encPass != null) {
                    val appLabel = packageName?.let { pkg ->
                        try {
                            val info = packageManager.getApplicationInfo(pkg, 0)
                            packageManager.getApplicationLabel(info).toString()
                        } catch (_: Exception) { null }
                    }

                    val title = AutofillTitleGenerator.getSmartTitle(
                        pageTitle = parser.pageTitle,
                        domain = webDomain,
                        appLabel = appLabel,
                        packageName = packageName
                    )

                    val entry = VaultEntry(
                        id = existing?.id ?: 0, // 核心修复：复用 ID 以实现更新
                        title = existing?.title ?: title, // 优先保留旧标题
                        username = encUser,
                        password = encPass,
                        category = existing?.category ?: "自动抓取",
                        associatedAppPackage = packageName,
                        associatedDomain = webDomain,
                        entryType = 0,
                        updatedAt = System.currentTimeMillis()
                    )

                    if (entry.id != 0) dao.update(entry) else dao.insert(entry)
                    Logcat.i(tag, "Successfully synchronized $title")
                }

                callback.onSuccess()
            } catch (e: Exception) {
                Logcat.e(tag, "Save failed", e)
                callback.onFailure(e.message)
            }
        }
    }
}
