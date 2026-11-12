package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.feature.vault.presentation.common.labelRes
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryAffectedFieldsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryDescriptionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryIcon
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryRevisionReference
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionFieldLabelUiState

internal fun EntryActivity.toDetailHistoryItemUiModel(): DetailHistoryItemUiModel {
    val presentation = activityType.toPresentation()
    return DetailHistoryItemUiModel(
        id = "activity:$activityId",
        description =
            when (val activityContext = context) {
                is EntryActivityContext.ContentRevision -> {
                    DetailHistoryDescriptionUiState.RevisionEvent(
                        labelRes = presentation.labelRes,
                        version = activityContext.version.value - EntryVersion.INITIAL.value,
                    )
                }

                EntryActivityContext.Usage, EntryActivityContext.Lifecycle -> {
                    DetailHistoryDescriptionUiState.Label(presentation.labelRes)
                }
            },
        icon = presentation.icon,
        filter = presentation.filter,
        affectedFields =
            affectedFields
                .map { DetailRevisionFieldLabelUiState.Resource(it.labelRes()) }
                .takeIf { it.isNotEmpty() }
                ?.let(DetailHistoryAffectedFieldsUiState::Labels)
                ?: DetailHistoryAffectedFieldsUiState.None,
        createdAtMs = createdAt,
        revision =
            when (val activityContext = context) {
                is EntryActivityContext.ContentRevision -> {
                    DetailHistoryRevisionReference.Pending(activityContext.version.value)
                }
                EntryActivityContext.Usage, EntryActivityContext.Lifecycle -> DetailHistoryRevisionReference.Unlinked
            },
    )
}

private data class ActivityPresentation(
    @get:StringRes val labelRes: Int,
    val icon: DetailHistoryIcon,
    val filter: DetailHistoryFilter,
)

private fun ActivityType.toPresentation(): ActivityPresentation =
    when (this) {
        ActivityType.CREATE -> {
            ActivityPresentation(
                R.string.vault_detail_activity_create,
                DetailHistoryIcon.CREATED,
                DetailHistoryFilter.CONTENT_CHANGES,
            )
        }

        ActivityType.UPDATE -> {
            ActivityPresentation(
                R.string.vault_detail_activity_update,
                DetailHistoryIcon.EDITED,
                DetailHistoryFilter.CONTENT_CHANGES,
            )
        }

        ActivityType.VERSION_RESTORED -> {
            ActivityPresentation(
                R.string.vault_detail_activity_version_restored,
                DetailHistoryIcon.VERSION_RESTORED,
                DetailHistoryFilter.CONTENT_CHANGES,
            )
        }

        ActivityType.DELETE -> {
            ActivityPresentation(
                R.string.vault_detail_activity_delete,
                DetailHistoryIcon.DELETED,
                DetailHistoryFilter.LIFECYCLE,
            )
        }

        ActivityType.RESTORE -> {
            ActivityPresentation(
                R.string.vault_detail_activity_restore,
                DetailHistoryIcon.TRASH_RESTORED,
                DetailHistoryFilter.LIFECYCLE,
            )
        }

        ActivityType.AUTOFILL -> {
            ActivityPresentation(
                R.string.vault_detail_activity_autofill,
                DetailHistoryIcon.AUTOFILLED,
                DetailHistoryFilter.USAGE,
            )
        }

        ActivityType.COPY_FIELD -> {
            ActivityPresentation(
                R.string.vault_detail_activity_copy_field,
                DetailHistoryIcon.COPIED,
                DetailHistoryFilter.USAGE,
            )
        }

        ActivityType.COPY_OTP_CODE -> {
            ActivityPresentation(
                R.string.vault_detail_activity_copy_otp_code,
                DetailHistoryIcon.COPIED,
                DetailHistoryFilter.USAGE,
            )
        }

        ActivityType.REVEAL_FIELDS -> {
            ActivityPresentation(
                R.string.vault_detail_activity_reveal_fields,
                DetailHistoryIcon.REVEALED,
                DetailHistoryFilter.USAGE,
            )
        }

        ActivityType.VIEW -> {
            ActivityPresentation(
                R.string.vault_detail_activity_view,
                DetailHistoryIcon.VIEWED,
                DetailHistoryFilter.USAGE,
            )
        }

        ActivityType.EXPORT -> {
            ActivityPresentation(
                R.string.vault_detail_activity_export,
                DetailHistoryIcon.EXPORTED,
                DetailHistoryFilter.USAGE,
            )
        }

        ActivityType.IMPORT -> {
            ActivityPresentation(
                R.string.vault_detail_activity_import,
                DetailHistoryIcon.IMPORTED,
                DetailHistoryFilter.USAGE,
            )
        }
    }
