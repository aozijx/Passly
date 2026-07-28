package com.aozijx.passly.core.autofill.model

import com.aozijx.passly.domain.settings.model.AutofillPresentation

/**
 * 解耦系统 API 的纯数据填充响应。
 *
 * 不包含 Dataset / RemoteViews / PendingIntent 等 Android 系统类型。
 * 由适配器层（LegacyResponseFactory / CredentialResponseFactory）转换为系统特定响应。
 */
data class InternalFillResponse(
    val candidates: List<ResolvedCandidate> = emptyList(),
    /** 填入 origin（用于 CredentialManager），可选 */
    val origin: String? = null,
    val availability: FillAvailability = FillAvailability.READY,
    val requireAuthentication: Boolean = true,
    val savePromptsEnabled: Boolean = false,
    val presentation: AutofillPresentation = AutofillPresentation.SYSTEM_INLINE,
)

enum class FillAvailability {
    READY,
    LOCKED,
    DISABLED,
    NO_MATCH,
    UNSUPPORTED_FIELDS,
}
