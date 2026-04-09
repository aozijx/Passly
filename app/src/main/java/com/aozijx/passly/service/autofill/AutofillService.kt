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
import com.aozijx.passly.AppContext
import com.aozijx.passly.R
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.crypto.CryptoAccess
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.platform.PackageUtils
import com.aozijx.passly.domain.model.AutofillMatchType
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry
import com.aozijx.passly.service.autofill.engine.AutofillStructureParser
import com.aozijx.passly.service.autofill.presentation.AutofillRemoteViewFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Passly 自动填充服务
 */
class AutofillService : android.service.autofill.AutofillService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val autofillRepository = AppContainer.autofillUseCases
    private val tag = "PasslyAutofill"
    private val slowFillTotalMs = 250L
    private val slowRepositoryMs = 120L
    private val slowDatasetBuildMs = 120L
    private val slowSaveMs = 180L

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.last().structure

        serviceScope.launch {
            try {
                val fillStart = System.currentTimeMillis()
                EntryTypeStrategyRegistry.ensureRegistered()
                val parser = AutofillStructureParser(structure)
                val autofillUiMode = AppContext.get().preference.autofillUiMode.first()
                Logcat.d(tag, "onFillRequest: pkg=${parser.packageName}, domain=${parser.webDomain}")

                val availableIds = listOfNotNull(parser.usernameId, parser.passwordId, parser.otpId)
                Logcat.d(
                    tag,
                    "parsed ids: username=${parser.usernameId != null}, password=${parser.passwordId != null}, otp=${parser.otpId != null}, available=${availableIds.size}"
                )
                if (availableIds.isEmpty()) {
                    Logcat.w(tag, "No autofill ids found; skip suggestions for this request")
                    callback.onSuccess(null)
                    return@launch
                }

                val repositoryStart = System.currentTimeMillis()
                val candidates = autofillRepository.findMatchingCandidates(
                    packageName = parser.normalizedPackageName,
                    webDomain = parser.normalizedWebDomain
                )
                val repositoryCost = System.currentTimeMillis() - repositoryStart
                if (repositoryCost >= slowRepositoryMs) {
                    Logcat.w(tag, "onFillRequest repository slow: ${repositoryCost}ms, entries=${candidates.size}")
                }

                val responseBuilder = FillResponse.Builder()
                val buildStart = System.currentTimeMillis()

                if (autofillUiMode == AutofillUiMode.BOTTOM_SHEET && candidates.isNotEmpty()) {
                    // BOTTOM_SHEET 模式：展示单条"踏板"，点击后弹出半屏候选列表
                    val presentation = AutofillRemoteViewFactory.createBottomSheetTrigger(
                        context = applicationContext,
                        candidateCount = candidates.size
                    )

                    val authIntent = Intent(this@AutofillService, AutofillAuthActivity::class.java).apply {
                        putExtra("vault_item_ids", candidates.map { it.entry.id }.toIntArray())
                        putExtra("username_id", parser.usernameId)
                        putExtra("password_id", parser.passwordId)
                        putExtra("otp_id", parser.otpId)
                        putExtra("autofill_ui_mode", autofillUiMode.key)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        this@AutofillService,
                        System.nanoTime().toInt(),
                        authIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    @Suppress("DEPRECATION")
                    responseBuilder.setAuthentication(
                        availableIds.toTypedArray(),
                        pendingIntent.intentSender,
                        presentation
                    )
                } else {
                    // SYSTEM_INLINE 模式：为每条候选账号生成独立 dataset 项
                    candidates.forEach { candidate ->
                        val entry = candidate.entry
                        val strategy = resolveStrategy(entry.entryType)
                        if (strategy != null && !strategy.supportsAutofill()) return@forEach

                        val decryptedUsername = (CryptoAccess.decryptOrNull(entry.username) ?: "").trim()
                        val strategySummary = strategy
                            ?.let { runCatching { it.extractSummary(entry) }.getOrDefault("") }
                            .orEmpty()
                        val subtitle = buildDatasetSubtitle(entry, decryptedUsername, strategySummary)
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

                        val authIntent = Intent(this@AutofillService, AutofillAuthActivity::class.java).apply {
                            putExtra("vault_item_id", entry.id)
                            putExtra("username_id", parser.usernameId)
                            putExtra("password_id", parser.passwordId)
                            putExtra("otp_id", parser.otpId)
                            putExtra("autofill_ui_mode", autofillUiMode.key)
                        }

                        val pendingIntent = PendingIntent.getActivity(
                            this@AutofillService,
                            entry.id.hashCode() xor System.nanoTime().toInt(),
                            authIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val datasetBuilder = Dataset.Builder().setAuthentication(pendingIntent.intentSender)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val field = Field.Builder().setPresentations(
                                Presentations.Builder().setMenuPresentation(presentation).build()
                            ).build()
                            availableIds.forEach { datasetBuilder.setField(it, field) }
                        } else {
                            @Suppress("DEPRECATION")
                            availableIds.forEach { id -> datasetBuilder.setValue(id, null, presentation) }
                        }

                        responseBuilder.addDataset(datasetBuilder.build())
                    }
                }

                val buildCost = System.currentTimeMillis() - buildStart
                if (buildCost >= slowDatasetBuildMs) {
                    Logcat.w(tag, "onFillRequest dataset build slow: ${buildCost}ms, entries=${candidates.size}")
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
                    val appLabel = appData?.first ?: parser.webDomain
                        ?: getString(R.string.autofill_title_app_fallback)
                    val iconBitmap = appData?.second?.let { PackageUtils.drawableToBitmap(it) }

                    if (iconBitmap != null) {
                        val customDescription = CustomDescription.Builder(
                            AutofillRemoteViewFactory.createSaveDescription(
                                context = applicationContext,
                                appLabel = appLabel,
                                iconBitmap = iconBitmap
                            )
                        ).build()
                        saveInfoBuilder.setCustomDescription(customDescription)
                    } else {
                        saveInfoBuilder.setDescription(
                            getString(R.string.autofill_save_prompt_description, appLabel)
                        )
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
                callback.onSuccess(if (candidates.isNotEmpty() || saveIds.isNotEmpty()) response else null)

                val totalCost = System.currentTimeMillis() - fillStart
                if (totalCost >= slowFillTotalMs) {
                    Logcat.w(tag, "onFillRequest slow total: ${totalCost}ms, entries=${candidates.size}, saveIds=${saveIds.size}")
                }

            } catch (e: Exception) {
                Logcat.e(tag, "Fill request failed", e)
                callback.onFailure(e.message)
            }
        }
    }

    private fun resolveStrategy(entryTypeValue: Int) = runCatching {
        EntryTypeStrategyFactory.getStrategy(EntryType.fromValue(entryTypeValue))
    }.getOrNull()

    private fun buildDatasetSubtitle(
        entry: VaultEntry,
        decryptedUsername: String,
        strategySummary: String
    ): String {
        val infoParts = mutableListOf<String>()
        if (decryptedUsername.isNotBlank()) infoParts += decryptedUsername
        if (strategySummary.isNotBlank()) infoParts += strategySummary
        if (infoParts.isEmpty()) infoParts += EntryType.fromValue(entry.entryType).displayName
        val joined = infoParts.joinToString(" · ")
        return if (!entry.totpSecret.isNullOrBlank()) "OTP · $joined" else joined
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        var username = ""
        var password = ""
        var pkg: String? = null
        var domain: String? = null
        var title: String? = null

        request.fillContexts.forEach { context ->
            val p = AutofillStructureParser(context.structure)
            if (pkg == null) pkg = p.packageName
            if (domain == null) domain = p.webDomain
            if (title == null) title = p.pageTitle
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
                val saveStart = System.currentTimeMillis()
                val success = autofillRepository.saveOrUpdateEntry(
                    packageName = pkg,
                    webDomain = domain,
                    pageTitle = title,
                    usernameValue = username,
                    passwordValue = password
                )
                if (success) {
                    Logcat.i(tag, "Successfully saved credentials for $username")
                    callback.onSuccess()
                } else {
                    Logcat.e(tag, "Failed to save credentials")
                    callback.onFailure("Save failed in repository")
                }
                val saveCost = System.currentTimeMillis() - saveStart
                if (saveCost >= slowSaveMs) Logcat.w(tag, "onSaveRequest slow: ${saveCost}ms")
            } catch (e: Exception) {
                Logcat.e(tag, "Exception during save", e)
                callback.onFailure(e.message)
            }
        }
    }
}
