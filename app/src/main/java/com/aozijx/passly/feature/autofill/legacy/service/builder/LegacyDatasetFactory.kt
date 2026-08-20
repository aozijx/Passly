package com.aozijx.passly.feature.autofill.legacy.service.builder

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
 * Legacy Dataset Factory for building individual Autofill datasets.
 */
internal object LegacyDatasetFactory {

    fun createFillDatasetForRoles(
        usernameIds: List<AutofillId>,
        passwordIds: List<AutofillId>,
        otpIds: List<AutofillId>,
        username: String,
        password: String,
        totpCode: String?,
        presentation: RemoteViews? = null,
        existingBuilder: Dataset.Builder? = null
    ): Dataset? {
        val builder = existingBuilder ?: Dataset.Builder()
        var added = false

        fun addField(id: AutofillId, text: String?) {
            if (text.isNullOrBlank()) return
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

        usernameIds.distinct().forEach { addField(it, username) }
        passwordIds.distinct().forEach { addField(it, password) }
        otpIds.distinct().forEach { addField(it, totpCode) }

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
