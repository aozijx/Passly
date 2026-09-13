package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailEntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailActivityTypeUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailActivityUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailContentUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailHeaderUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailMetadataUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailOtpUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.RelatedEntryUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.ScopedSensitiveText

internal fun detailHeaderUiModel(
    entry: Entry,
    state: DetailUiState,
) = DetailHeaderUiModel(
    title = entry.title,
    favorite = entry.favorite,
    editedTitle = state.editedTitle,
    isEditingTitle = state.isEditingTitle,
)

internal fun detailContentUiModel(
    entry: Entry,
    state: DetailUiState,
) = DetailContentUiModel(
    relatedEntries = state.relatedEntries.map {
        RelatedEntryUiModel(it.id.value, it.title, DetailEntryTypeUiModel.valueOf(it.type.name))
    },
    metadata = DetailMetadataUiModel(entry.createdAt, entry.updatedAt),
    activities = state.history.map {
        DetailActivityUiModel(
            type = DetailActivityTypeUiModel.valueOf(it.activityType.name),
            source = it.source,
            createdAt = it.createdAt,
        )
    },
)

internal fun detailOtpUiModel(otp: OtpCodeState?): DetailOtpUiModel? = otp?.let {
    DetailOtpUiModel(it.code, it.progress, it.isLoading, it.error != null)
}

internal fun SensitiveValue?.asScopedSensitiveText(): ScopedSensitiveText {
    val source = this ?: return ScopedSensitiveText.Empty
    return object : ScopedSensitiveText {
        override val isEmpty: Boolean get() = source.isEmpty

        override fun <R> useChars(block: (CharArray) -> R): R {
            val chars = source.toCharArray()
            return try {
                block(chars)
            } finally {
                chars.fill('\u0000')
            }
        }

        override fun toString() = "***"
    }
}
