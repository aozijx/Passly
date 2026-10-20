package com.aozijx.passly.feature.autofill.legacy.service.builder

import android.service.autofill.SaveInfo
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryContext
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure

object LegacySaveInfoFactory {
    fun create(
        discoveryContext: AutofillDiscoveryContext,
        parsed: ParsedStructure,
    ): SaveInfo? {
        if (!discoveryContext.savePromptsEnabled) return null
        val plan = LegacySaveInfoPolicy.plan(discoveryContext.roleMap) ?: return null
        val idByString = parsed.editableFields.associate { field ->
            field.autofillId.toString() to field.autofillId
        }
        val requiredPasswordIds = plan.requiredPasswordFieldIds.mapNotNull(idByString::get)
        if (requiredPasswordIds.isEmpty()) return null
        val optionalUsernameIds = plan.optionalUsernameFieldIds.mapNotNull(idByString::get)
        val dataTypes = SaveInfo.SAVE_DATA_TYPE_PASSWORD or
            if (optionalUsernameIds.isNotEmpty()) SaveInfo.SAVE_DATA_TYPE_USERNAME else 0

        return SaveInfo.Builder(dataTypes, requiredPasswordIds.toTypedArray()).apply {
            if (optionalUsernameIds.isNotEmpty()) {
                setOptionalIds(optionalUsernameIds.toTypedArray())
            }
            setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
        }.build()
    }
}
