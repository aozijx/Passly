package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionResolver
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailEntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailActivityTypeUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailActivityUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailMetadataUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailNotesUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailOtpUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailScreenUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSectionKindUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSectionUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.RelatedEntryUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.ScopedSensitiveText

internal fun detailScreenUiModel(
    entry: Entry,
    state: DetailUiState,
    otp: OtpCodeState?,
) = DetailScreenUiModel(
    entryId = entry.id.value,
    title = entry.title,
    username = entry.username,
    entryType = DetailEntryTypeUiModel.valueOf(entry.type.name),
    favorite = entry.favorite,
    iconName = entry.icon.name,
    iconCustomPath = entry.iconCustomPath,
    associatedDomain = entry.associatedDomain,
    associatedAppPackage = entry.associatedAppPackage,
    editedTitle = state.editedTitle,
    isEditingTitle = state.isEditingTitle,
    validationError = state.validationError,
    isAccessHistoryEnabled = state.isAccessHistoryEnabled,
    isFaviconDownloading = state.isFaviconDownloading,
    sections = DetailSectionResolver.resolve(entry).map {
        DetailSectionUiModel(DetailSectionKindUiModel.valueOf(it.name))
    },
    relatedEntries = state.relatedEntries.map {
        RelatedEntryUiModel(it.id.value, it.title, DetailEntryTypeUiModel.valueOf(it.type.name))
    },
    associatedInfo = DetailAssociatedInfoUiModel(
        domain = entry.associatedDomain,
        applicationIds = entry.associations.applicationIds.sorted(),
        isEditingDomain = false,
        isFaviconDownloading = state.isFaviconDownloading,
    ),
    notes = DetailNotesUiModel(entry.secret.notes, entry.secret.notes.orEmpty(), false),
    metadata = DetailMetadataUiModel(entry.createdAt, entry.updatedAt),
    activities = state.history.map {
        DetailActivityUiModel(
            type = DetailActivityTypeUiModel.valueOf(it.activityType.name),
            source = it.source,
            createdAt = it.createdAt,
        )
    },
    otp = otp?.let { DetailOtpUiModel(it.code, it.progress, it.isLoading, it.error != null) },
)

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
