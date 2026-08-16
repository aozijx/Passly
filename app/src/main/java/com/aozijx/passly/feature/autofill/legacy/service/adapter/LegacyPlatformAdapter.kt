package com.aozijx.passly.feature.autofill.legacy.service.adapter

import android.content.Context
import android.service.autofill.FillResponse
import com.aozijx.passly.core.autofill.model.FieldDescriptor
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy 平台适配器：负责 Android AutofillService API 与核心 FillPipeline 之间的双向转换。
 *
 * - [buildRequest]：ParsedStructure → InternalFillRequest（Android Id → 纯数据）
 * - [buildResponse]：InternalFillResponse → FillResponse（纯数据 → Android Dataset/FillResponse）
 *
 * 字段描述透传 Parser 收集到的真实属性（inputType/hint/className/resourceId），
 * 保证启发式匹配能识别样式化/自定义控件，而不是只依赖硬编码 hint。
 */
@Singleton
class LegacyPlatformAdapter @Inject constructor() {

    fun buildRequest(parsed: ParsedStructure): InternalFillRequest {
        // 优先使用解析出的全部可编辑字段；解析失败时回退到识别出的角色字段。
        val fields = parsed.editableFields.map { field ->
            FieldDescriptor(
                viewId = field.autofillId.toString(),
                autofillHints = field.autofillHints,
                resourceId = field.resourceId,
                inputType = field.inputType,
                hint = field.hint,
                contentDescription = field.contentDescription,
                className = field.className,
            )
        }.ifEmpty {
            buildRoleFallbackFields(parsed)
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

    private fun buildRoleFallbackFields(parsed: ParsedStructure): List<FieldDescriptor> =
        buildList {
            if (parsed.usernameId != null) {
                add(
                    FieldDescriptor(
                        viewId = parsed.usernameId.toString(),
                        autofillHints = listOf("USERNAME"),
                        resourceId = parsed.usernameResourceId,
                    )
                )
            }
            if (parsed.passwordId != null) {
                add(
                    FieldDescriptor(
                        viewId = parsed.passwordId.toString(),
                        autofillHints = listOf("PASSWORD"),
                        resourceId = parsed.passwordResourceId,
                    )
                )
            }
            if (parsed.otpId != null) {
                add(
                    FieldDescriptor(
                        viewId = parsed.otpId.toString(),
                        autofillHints = listOf("ONE_TIME_CODE"),
                        resourceId = parsed.otpResourceId,
                    )
                )
            }
        }
}
