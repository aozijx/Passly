package com.example.poop.service.autofill

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.core.graphics.createBitmap
import com.example.poop.R
import com.example.poop.ui.screens.vault.utils.AutofillParser
import com.example.poop.util.Logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Poop 自动填充服务 - 增强版
 * 优化重点：通过 FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE 和 TriggerId 提升保存弹出率
 */
class AutofillService : android.service.autofill.AutofillService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "PoopAutofill"
    
    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val parser = AutofillParser(request.fillContexts.last().structure)
        
        Logcat.d(tag, "onFillRequest: pkg=${parser.packageName}, domain=${parser.webDomain}")

        val availableIds = listOfNotNull(parser.usernameId, parser.passwordId, parser.otpId)
        
        serviceScope.launch {
            try {
                val entries = AutofillRepository.findMatchingEntries(applicationContext, parser.packageName, parser.webDomain)
                val responseBuilder = FillResponse.Builder()

                // 1. 处理填充建议 (Datasets)
                entries.forEach { entry ->
                    val presentation = RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                        setTextViewText(R.id.title, entry.title)
                        setTextViewText(R.id.username, if (entry.totpSecret.isNullOrBlank()) "点击填充" else "填充账号与验证码")
                        val appPkg = entry.associatedAppPackage
                        if (!appPkg.isNullOrEmpty()) {
                            try {
                                val icon = packageManager.getApplicationIcon(appPkg)
                                setImageViewBitmap(R.id.icon, drawableToBitmap(icon))
                            } catch (_: Exception) {}
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

                // 2. 核心：精准保存策略
                val saveIds = listOfNotNull(parser.usernameId, parser.passwordId)
                if (saveIds.isNotEmpty()) {
                    val saveInfoBuilder = SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME,
                        saveIds.toTypedArray()
                    )

                    // 获取应用名称，让提示更明显
                    val appLabel = try {
                        parser.packageName?.let { pkg ->
                            val info = packageManager.getApplicationInfo(pkg, 0)
                            packageManager.getApplicationLabel(info).toString()
                        } ?: parser.webDomain ?: "该应用"
                    } catch (_: Exception) { "该应用" }
                    // --- 新增代码：配置带图标的自定义保存提示 ---
                    val iconBitmap = try {
                        parser.packageName?.let { pkg ->
                            drawableToBitmap(packageManager.getApplicationIcon(pkg))
                        }
                    } catch (_: Exception) { null }

                    if (iconBitmap != null) {
                        // 如果有应用图标且系统支持，则使用 CustomDescription 显示图标和文字
                        val customDescription = CustomDescription.Builder(
                            RemoteViews(packageName, R.layout.autofill_save_description).apply {
                                setImageViewBitmap(R.id.save_icon, iconBitmap)
                                setTextViewText(R.id.save_title
                                    , "保存到 Poop？")
                                setTextViewText(R.id.save_description, "是否保存 $appLabel 的账号密码？")
                            }
                        ).build()
                        saveInfoBuilder.setCustomDescription(customDescription)
                    } else {
                        // 设置描述：让用户知道保存的是哪个应用 (原有逻辑作为回退方案)
                        saveInfoBuilder.setDescription("是否将 $appLabel 的账号密码保存到 Poop？")
                    }
                    var saveFlags = SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE
                    
                    // 策略 B: 延迟保存 (解决分步登录，如先输账号点下一步再输密码)
                    if (parser.usernameId != null && parser.passwordId == null) {
                        saveFlags = saveFlags or SaveInfo.FLAG_DELAY_SAVE
                    }
                    saveInfoBuilder.setFlags(saveFlags)

                    // 策略 C: 绑定登录按钮作为显式触发器
                    parser.submitId?.let {
                        Logcat.d(tag, "Binding save trigger to button: $it")
                        saveInfoBuilder.setTriggerId(it)
                    }

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

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1))
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        var username = ""
        var password = ""
        var pkg: String? = null
        var domain: String? = null
        var title: String? = null

        request.fillContexts.forEach { context ->
            val p = AutofillParser(context.structure)
            if (!p.usernameValue.isNullOrBlank()) username = p.usernameValue!!
            if (!p.passwordValue.isNullOrBlank()) password = p.passwordValue!!
            p.packageName?.let { pkg = it }
            p.webDomain?.let { domain = it }
            p.pageTitle?.let { title = it }
        }

        if (password.isBlank()) return callback.onSuccess()

        serviceScope.launch {
            val success = AutofillRepository.saveOrUpdateEntry(applicationContext, pkg, domain, title, username, password)
            if (success) callback.onSuccess() else callback.onFailure("Save failed")
        }
    }
}
