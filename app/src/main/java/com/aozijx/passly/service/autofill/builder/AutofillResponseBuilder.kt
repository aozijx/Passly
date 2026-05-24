package com.aozijx.passly.service.autofill.builder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.service.autofill.CustomDescription
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillResponse
import android.service.autofill.Presentations
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.compose.ui.graphics.asAndroidBitmap
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.encryption.CryptoAccess
import com.aozijx.passly.core.otp.TwoFAUtils
import com.aozijx.passly.core.platform.PackageUtils
import com.aozijx.passly.domain.config.AutofillUiMode
import com.aozijx.passly.domain.model.AutofillCandidate
import com.aozijx.passly.domain.model.AutofillMatchType
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.service.autofill.AutofillAuthActivity
import com.aozijx.passly.service.autofill.credential.AutofillCredentialProvider
import com.aozijx.passly.service.autofill.parser.AutofillStructureParser
import com.aozijx.passly.service.autofill.presenter.AutofillRemoteViewFactory

internal object AutofillResponseBuilder {

    fun buildUnlockResponse(
        context: Context,
        parser: AutofillStructureParser,
        ids: Array<AutofillId>,
        uiMode: AutofillUiMode
    ): FillResponse {
        val presentation = AutofillRemoteViewFactory.createUnlockTrigger(context)
        val pendingIntent = createAuthIntent(
            context = context,
            isUnlockOnly = true,
            parser = parser,
            uiMode = uiMode,
            packageName = parser.normalizedPackageName,
            webDomain = parser.normalizedWebDomain
        )
        val builder = FillResponse.Builder()
        builder.setAuthenticationCompat(ids, pendingIntent.intentSender, presentation)
        return builder.build()
    }

    suspend fun buildFillResponse(
        context: Context,
        candidates: List<AutofillCandidate>,
        parser: AutofillStructureParser,
        uiMode: AutofillUiMode,
        availableIds: List<AutofillId>
    ): FillResponse {
        val builder = FillResponse.Builder()

        if (uiMode == AutofillUiMode.BOTTOM_SHEET && candidates.isNotEmpty()) {
            applyBottomSheetAuth(
                context = context,
                candidateIds = candidates.map { it.entry.id }.toIntArray(),
                parser = parser,
                ids = availableIds.toTypedArray(),
                uiMode = uiMode,
                builder = builder
            )
        } else {
            candidates.forEach { candidate ->
                val dataset = createInlineDataset(context, candidate, parser, availableIds, uiMode)
                if (dataset != null) builder.addDataset(dataset)
            }
        }

        val saveInfo = buildSaveInfo(context, parser)
        if (saveInfo != null) builder.setSaveInfo(saveInfo)

        return builder.build()
    }

    fun buildPostUnlockFillResponse(
        context: Context,
        candidates: List<AutofillCandidate>,
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        otpId: AutofillId?
    ): FillResponse? {
        val builder = FillResponse.Builder()
        var datasetCount = 0

        candidates.forEach { candidate ->
            val entry = candidate.entry
            val decryptedUsername = (CryptoAccess.decryptOrNull(entry.username) ?: "").trim()
            val subtitle = AutofillCredentialProvider.buildSubtitle(entry, decryptedUsername)
            val badge = when (candidate.matchType) {
                AutofillMatchType.APP -> context.getString(R.string.autofill_match_app)
                AutofillMatchType.DOMAIN -> context.getString(R.string.autofill_match_domain)
                AutofillMatchType.UNKNOWN -> context.getString(R.string.autofill_match_unknown)
            }
            val presentation = AutofillRemoteViewFactory.createDatasetItem(
                context = context, entry = entry, subtitle = subtitle, badge = badge
            )

            val basicCred = AutofillCredentialProvider.getBasicCredentials(entry)
            if (basicCred != null) {
                val totpCode = if (otpId != null && entry.totpSecret?.isNotBlank() == true) {
                    TwoFAUtils.generateCurrentTotpFromEntry(entry)
                } else null

                val dataset = createFillDataset(
                    usernameId = usernameId,
                    passwordId = passwordId,
                    otpId = otpId,
                    username = basicCred.username,
                    password = basicCred.password,
                    totpCode = totpCode,
                    presentation = presentation
                )
                if (dataset != null) {
                    builder.addDataset(dataset)
                    datasetCount++
                }
            }
        }

        return if (datasetCount > 0) builder.build() else null
    }

    fun createFillDataset(
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        otpId: AutofillId?,
        username: String,
        password: String,
        totpCode: String?,
        presentation: RemoteViews? = null
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
                        Presentations.Builder().setMenuPresentation(presentation).build()
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

    fun FillResponse.Builder.setAuthenticationCompat(
        ids: Array<AutofillId>,
        intentSender: IntentSender,
        presentation: RemoteViews
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val presentations = Presentations.Builder().setMenuPresentation(presentation).build()
            setAuthentication(ids, intentSender, presentations)
        } else {
            @Suppress("DEPRECATION")
            setAuthentication(ids, intentSender, presentation)
        }
    }

    fun Dataset.Builder.setMenuPresentationCompat(
        ids: List<AutofillId>,
        presentation: RemoteViews
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val field = Field.Builder().setPresentations(
                Presentations.Builder().setMenuPresentation(presentation).build()
            ).build()
            ids.forEach { setField(it, field) }
        } else {
            @Suppress("DEPRECATION")
            ids.forEach { id -> setValue(id, null, presentation) }
        }
    }

    private fun createAuthIntent(
        context: Context,
        entryId: Int? = null,
        candidateIds: IntArray? = null,
        isUnlockOnly: Boolean = false,
        parser: AutofillStructureParser,
        uiMode: AutofillUiMode,
        packageName: String? = null,
        webDomain: String? = null
    ): PendingIntent {
        val intent = Intent(context, AutofillAuthActivity::class.java).apply {
            if (isUnlockOnly) putExtra("unlock_only", true)
            entryId?.let { putExtra("vault_item_id", it) }
            candidateIds?.let { putExtra("vault_item_ids", it) }
            putExtra("username_id", parser.usernameId)
            putExtra("password_id", parser.passwordId)
            putExtra("otp_id", parser.otpId)
            putExtra("autofill_ui_mode", uiMode.name)
            packageName?.let { putExtra("package_name", it) }
            webDomain?.let { putExtra("web_domain", it) }
        }
        val requestCode = entryId?.hashCode() ?: System.nanoTime().toInt()
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun applyBottomSheetAuth(
        context: Context,
        candidateIds: IntArray,
        parser: AutofillStructureParser,
        ids: Array<AutofillId>,
        uiMode: AutofillUiMode,
        builder: FillResponse.Builder
    ) {
        val presentation = AutofillRemoteViewFactory.createBottomSheetTrigger(
            context = context, candidateCount = candidateIds.size
        )
        val pendingIntent = createAuthIntent(
            context = context,
            candidateIds = candidateIds,
            parser = parser,
            uiMode = uiMode
        )
        builder.setAuthenticationCompat(ids, pendingIntent.intentSender, presentation)
    }

    private fun createInlineDataset(
        context: Context,
        candidate: AutofillCandidate,
        parser: AutofillStructureParser,
        availableIds: List<AutofillId>,
        uiMode: AutofillUiMode
    ): Dataset? {
        val entry = candidate.entry
        val strategy = resolveEntryTypeStrategy(entry.entryType)
        if (strategy != null && !strategy.supportsAutofill()) return null

        val decryptedUsername = (CryptoAccess.decryptOrNull(entry.username) ?: "").trim()
        val subtitle = AutofillCredentialProvider.buildSubtitle(entry, decryptedUsername)
        val badge = when (candidate.matchType) {
            AutofillMatchType.APP -> context.getString(R.string.autofill_match_app)
            AutofillMatchType.DOMAIN -> context.getString(R.string.autofill_match_domain)
            AutofillMatchType.UNKNOWN -> context.getString(R.string.autofill_match_unknown)
        }
        val presentation = AutofillRemoteViewFactory.createDatasetItem(
            context = context, entry = entry, subtitle = subtitle, badge = badge
        )
        val pendingIntent = createAuthIntent(
            context = context,
            entryId = entry.id,
            parser = parser,
            uiMode = uiMode
        )
        val datasetBuilder = Dataset.Builder().setAuthentication(pendingIntent.intentSender)
        datasetBuilder.setMenuPresentationCompat(availableIds, presentation)
        return datasetBuilder.build()
    }

    private suspend fun buildSaveInfo(
        context: Context,
        parser: AutofillStructureParser
    ): SaveInfo? {
        val saveIds = listOfNotNull(parser.usernameId, parser.passwordId)
        if (saveIds.isEmpty()) return null

        val saveInfoBuilder = SaveInfo.Builder(
            SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME,
            saveIds.toTypedArray()
        )

        val pkgName = parser.packageName
        val appMetadata = pkgName?.let { PackageUtils.getAppMetadata(context, it) }
        val appLabel = appMetadata?.appName
            ?: parser.webDomain
            ?: context.getString(R.string.autofill_title_app_fallback)

        val iconBitmap = pkgName?.let { PackageUtils.loadIcon(context, it) }?.asAndroidBitmap()
        if (iconBitmap != null) {
            val customDescription = CustomDescription.Builder(
                AutofillRemoteViewFactory.createSaveDescription(
                    context = context, appLabel = appLabel, iconBitmap = iconBitmap
                )
            ).build()
            saveInfoBuilder.setCustomDescription(customDescription)
        } else {
            saveInfoBuilder.setDescription(
                context.getString(R.string.autofill_save_prompt_description, appLabel)
            )
        }

        var saveFlags = SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE
        if (parser.usernameId != null && parser.passwordId == null) {
            saveFlags = saveFlags or SaveInfo.FLAG_DELAY_SAVE
        }
        saveInfoBuilder.setFlags(saveFlags)
        parser.submitId?.let { saveInfoBuilder.setTriggerId(it) }
        return saveInfoBuilder.build()
    }

    private fun resolveEntryTypeStrategy(entryTypeValue: Int) = runCatching {
        EntryTypeStrategyFactory.getStrategy(EntryType.fromValue(entryTypeValue))
    }.getOrNull()
}