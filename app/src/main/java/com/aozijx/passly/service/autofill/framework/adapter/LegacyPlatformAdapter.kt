package com.aozijx.passly.service.autofill.framework.adapter

import android.content.Context
import android.service.autofill.FillResponse
import com.aozijx.passly.core.autofill.model.FieldDescriptor
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.data.settings.model.AutofillPresentation
import com.aozijx.passly.service.autofill.framework.builder.LegacyResponseFactory
import com.aozijx.passly.service.autofill.framework.parser.ParsedStructure
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy 平台适配器：负责 Android AutofillService API 与核心 FillPipeline 之间的双向转换。
 *
 * - [buildRequest]：ParsedStructure → InternalFillRequest（Android Id → 纯数据）
 * - [buildResponse]：InternalFillResponse → FillResponse（纯数据 → Android Dataset/FillResponse）
 *
 * Android API 版本升级时，只需替换此适配器，Dispatcher 和领域层不受影响。
 */
@Singleton
class LegacyPlatformAdapter @Inject constructor() {

    fun buildRequest(parsed: ParsedStructure): InternalFillRequest {
        val fields = mutableListOf<FieldDescriptor>()

        if (parsed.usernameId != null) {
            fields.add(
                FieldDescriptor(
                    viewId = parsed.usernameId.toString(),
                    autofillHints = listOf("USERNAME"),
                    resourceId = parsed.usernameResourceId,
                )
            )
        }
        if (parsed.passwordId != null) {
            fields.add(
                FieldDescriptor(
                    viewId = parsed.passwordId.toString(),
                    autofillHints = listOf("PASSWORD"),
                    resourceId = parsed.passwordResourceId,
                )
            )
        }
        if (parsed.otpId != null) {
            fields.add(
                FieldDescriptor(
                    viewId = parsed.otpId.toString(),
                    autofillHints = listOf("ONE_TIME_CODE"),
                    resourceId = parsed.otpResourceId,
                )
            )
        }

        return InternalFillRequest(
            parentPackage = parsed.packageName ?: "",
            webDomain = parsed.webDomain,
            activity = parsed.pageTitle,
            fields = fields,
        )
    }

    fun buildResponse(
        response: InternalFillResponse,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        context: Context,
    ): FillResponse {
        return LegacyResponseFactory.buildFillResponse(context, response, parsed, uiMode)
    }
}
