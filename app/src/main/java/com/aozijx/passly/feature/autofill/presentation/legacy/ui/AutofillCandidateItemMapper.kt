package com.aozijx.passly.feature.autofill.presentation.legacy.ui

import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary

fun AutofillCandidateSummary.toUiItem() = AutofillCandidateItem(
    id = entry.id,
    iconName = entry.profile.icon.name,
    iconCustomPath = entry.profile.icon.customReference,
    associatedAppPackage = entry.profile.associations.applicationIds.firstOrNull(),
    entryType = entry.entryType,
    title = entry.title,
    username = entry.username,
    associatedDomain = entry.associatedDomain,
)
