package com.aozijx.passly.presentation.feature.autofill.legacy.ui

import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.presentation.mapping.entry.toUiModel

fun AutofillCandidateSummary.toUiItem() = AutofillCandidateItem(
    id = entry.id,
    iconName = entry.profile.icon.name,
    iconCustomPath = entry.profile.icon.customReference,
    associatedAppPackage = entry.profile.associations.applicationIds.firstOrNull(),
    entryType = entry.entryType.toUiModel(),
    title = entry.title,
    username = entry.username,
    associatedDomain = entry.associatedDomain,
)
