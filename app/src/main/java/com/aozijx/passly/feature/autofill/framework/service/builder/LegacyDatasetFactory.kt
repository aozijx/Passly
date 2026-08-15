package com.aozijx.passly.feature.autofill.framework.service.builder

import android.content.IntentSender
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillResponse
import android.service.autofill.Presentations
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews

/**
 * Legacy Dataset 工厂：负责构建单个 Autofill Dataset 及版本兼容辅助方法。
 */
internal object LegacyDatasetFactory {

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

    fun setAuthenticationCompat(
        builder: FillResponse.Builder,
        ids: Array<AutofillId>,
        intentSender: IntentSender,
        presentation: RemoteViews
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val presentations = Presentations.Builder().setMenuPresentation(presentation).build()
            builder.setAuthentication(ids, intentSender, presentations)
        } else {
            @Suppress("DEPRECATION")
            builder.setAuthentication(ids, intentSender, presentation)
        }
    }

    fun setMenuPresentationCompat(
        builder: Dataset.Builder,
        ids: List<AutofillId>,
        presentation: RemoteViews
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val field = Field.Builder().setPresentations(
                Presentations.Builder().setMenuPresentation(presentation).build()
            ).build()
            ids.forEach { builder.setField(it, field) }
        } else {
            @Suppress("DEPRECATION")
            ids.forEach { id -> builder.setValue(id, null, presentation) }
        }
    }
}