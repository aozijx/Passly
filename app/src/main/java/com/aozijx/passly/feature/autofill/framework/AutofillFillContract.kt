package com.aozijx.passly.feature.autofill.framework

import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.settings.model.AutofillPresentation

sealed interface AutofillAuthenticationPayload {
    data class Response(val value: FillResponse) : AutofillAuthenticationPayload
    data class DatasetResult(val value: Dataset) : AutofillAuthenticationPayload
}

sealed class AutofillFillUiState {
    data object Initial : AutofillFillUiState()
    data object Loading : AutofillFillUiState()
    data class ShowCandidates(
        val candidates: List<ResolvedCandidate>
    ) : AutofillFillUiState()

    data class Result(
        val payload: AutofillAuthenticationPayload?
    ) : AutofillFillUiState()

    data class Error(val message: String) : AutofillFillUiState()
}

data class AutofillFillRequest(
    val uiMode: AutofillPresentation,
    val isUnlockOnly: Boolean,
    val usernameId: AutofillId?,
    val passwordId: AutofillId?,
    val otpId: AutofillId?,
    val packageName: String?,
    val webDomain: String?,
    val directEntryId: String?,
    val candidateEntryIds: List<String>,
    val returnsDataset: Boolean,
    /** 页面全部可编辑字段（样式化控件识别不出角色时的填充兜底顺序）。 */
    val editableIds: List<AutofillId> = emptyList(),
) {
    /** 有效填充目标字段：优先角色识别结果，否则回退到全部可编辑字段。 */
    val resolvedUsernameId: AutofillId?
        get() = usernameId ?: editableIds.getOrNull(0)
    val resolvedPasswordId: AutofillId?
        get() = passwordId ?: editableIds.getOrNull(1)
}
