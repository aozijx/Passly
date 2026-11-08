package com.aozijx.passly.feature.vault.presentation.trash

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryUiModel
import com.aozijx.passly.presentation.shared.components.EntryClassificationInput
import com.aozijx.passly.presentation.shared.components.EntryVisualCategoryClassifier
import com.aozijx.passly.presentation.shared.media.toLocalIconImageModel

internal object TrashUiMapper {
    fun map(entries: List<EntryListItem>): List<TrashEntryUiModel> =
        entries.map { entry ->
            TrashEntryUiModel(
                id = entry.id.value,
                version = entry.identity.version.value,
                title = entry.title,
                entryType = entry.entryType,
                supportingText =
                    entry.username.takeIf(String::isNotBlank)
                        ?: entry.associatedDomain?.takeIf(String::isNotBlank)
                        ?: entry.associatedAppPackage?.takeIf(String::isNotBlank),
                deletedAtEpochMs = entry.deletedAt,
                associatedDomain = entry.associatedDomain,
                associatedAppPackage = entry.associatedAppPackage,
                iconName = entry.icon.name,
                iconCustomPath = entry.iconCustomPath,
                iconImageModel = toLocalIconImageModel(entry.iconCustomPath),
                visualCategory =
                    EntryVisualCategoryClassifier.classify(
                        EntryClassificationInput(
                            entryType = entry.entryType,
                            title = entry.title,
                            username = entry.username,
                            urls = setOfNotNull(entry.associatedDomain),
                            domains = setOfNotNull(entry.associatedDomain),
                            packageNames = setOfNotNull(entry.associatedAppPackage),
                            appNames =
                                setOfNotNull(
                                    entry.title.takeIf { entry.associatedAppPackage != null && entry.associatedDomain == null },
                                ),
                        ),
                    ),
            )
        }
}
