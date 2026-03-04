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
import android.service.autofill.SaveRequest
import android.widget.RemoteViews
import com.example.poop.R
import com.example.poop.data.AppDatabase
import com.example.poop.ui.screens.vault.utils.AutofillParser
import com.example.poop.util.Logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Poop 自动填充服务
 * 增强版：支持多域名匹配、模糊匹配兜底及最近使用优先排序
 */
class PoopAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val tag = "PoopAutofill"

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val parser = AutofillParser(structure)

        if (parser.usernameId == null && parser.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val items = db.vaultDao().getAllItems().first()

                // 核心匹配与排序逻辑
                val filteredItems = items.filter { item ->
                    // 1. 包名或域名完全匹配
                    val directMatch = (item.associatedAppPackage != null && item.associatedAppPackage == parser.packageName) ||
                                     (item.associatedDomain != null && item.associatedDomain == parser.webDomain)

                    // 2. URI 列表匹配 (支持一个账号多个域名/App)
                    val uriMatch = item.uriList?.split(",")?.any { uri ->
                        val trimmed = uri.trim()
                        trimmed == parser.packageName || trimmed == parser.webDomain
                    } ?: false

                    // 3. 标题模糊匹配 (作为兜底)
                    val titleMatch = parser.packageName?.let { item.title.contains(it, ignoreCase = true) } ?: false ||
                                    parser.webDomain?.let { item.title.contains(it, ignoreCase = true) } ?: false

                    directMatch || uriMatch || titleMatch
                }.sortedByDescending {
                    // 核心排序：优先显示最近使用的条目 (lastUsedAt)
                    it.lastUsedAt ?: 0L
                }.take(5)

                if (filteredItems.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()

                filteredItems.forEach { item ->
                    val presentation = RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                        setTextViewText(R.id.title, item.title)
                        setTextViewText(R.id.username, "点击验证并填充")
                    }

                    val authIntent = Intent(
                        this@PoopAutofillService,
                        AutofillAuthActivity::class.java
                    ).apply {
                        putExtra("vault_item", item)
                        putExtra("username_id", parser.usernameId)
                        putExtra("password_id", parser.passwordId)
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        this@PoopAutofillService,
                        item.id,
                        authIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val datasetBuilder = Dataset.Builder()
                        .setAuthentication(pendingIntent.intentSender)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val field = Field.Builder()
                            .setPresentations(Presentations.Builder().setMenuPresentation(presentation).build())
                            .build()
                        parser.usernameId?.let { datasetBuilder.setField(it, field) }
                    } else {
                        @Suppress("DEPRECATION")
                        parser.usernameId?.let { datasetBuilder.setValue(it, null, presentation) }
                    }

                    responseBuilder.addDataset(datasetBuilder.build())
                }

                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                Logcat.e(tag, "Fill request failed", e)
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }
}