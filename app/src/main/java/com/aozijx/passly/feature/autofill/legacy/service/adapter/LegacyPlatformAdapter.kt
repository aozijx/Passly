package com.aozijx.passly.feature.autofill.legacy.service.adapter

import android.content.Context
import android.service.autofill.FillResponse
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillResponse
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy Platform Adapter: Converts between Android Autofill API and platform-agnostic models.
 */
@Singleton
class LegacyPlatformAdapter @Inject constructor() {

    fun buildRequest(parsed: ParsedStructure): AutofillRequest {
        val fields = parsed.editableFields.map { field ->
            AutofillField(
                id = field.autofillId.toString(),
                hints = field.autofillHints.toSet(),
                inputType = field.inputType,
                isFocused = field.isFocused,
                resourceId = field.resourceId,
                contentDescription = field.contentDescription,
                className = field.className,
                text = field.value,
                hint = field.hint
            )
        }

        return AutofillRequest(
            packageName = parsed.packageName ?: "",
            domain = parsed.webDomain,
            fields = fields,
            source = AutofillSource.AUTOFILL_SERVICE,
            activityTitle = parsed.pageTitle
        )
    }

    fun buildResponse(
        response: AutofillResponse,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        context: Context,
    ): FillResponse {
        return LegacyResponseFactory.buildFillResponse(context, response, parsed, uiMode)
    }
}
