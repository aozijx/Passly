package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
object EntryRevisionComparator {
    fun compare(
        historical: RedactedEntryRevision,
        current: RedactedEntryRevision,
    ): List<EntryRevisionDifference> = buildList {
        addDifference(RevisionFieldId.Title, historical.profile.title.text(), current.profile.title.text())
        addDifference(RevisionFieldId.Username, historical.profile.username.text(), current.profile.username.text())
        addDifference(
            RevisionFieldId.PrimaryUrl,
            historical.profile.associations.primaryUrl.text(),
            current.profile.associations.primaryUrl.text(),
        )
        addDifference(
            RevisionFieldId.Domains,
            historical.profile.associations.domains.textSet(),
            current.profile.associations.domains.textSet(),
        )
        addDifference(
            RevisionFieldId.ApplicationIds,
            historical.profile.associations.applicationIds.textSet(),
            current.profile.associations.applicationIds.textSet(),
        )
        addDifference(
            RevisionFieldId.Icon,
            RevisionFieldValue.Icon(historical.profile.icon),
            RevisionFieldValue.Icon(current.profile.icon),
        )
        addDifference(
            RevisionFieldId.Favorite,
            RevisionFieldValue.Flag(historical.profile.favorite),
            RevisionFieldValue.Flag(current.profile.favorite),
        )
        addDifference(RevisionFieldId.Tags, historical.profile.tags.textSet(), current.profile.tags.textSet())
        addDifference(
            RevisionFieldId.ExpiresAt,
            historical.profile.expiresAtMs?.let(RevisionFieldValue::EpochMillis),
            current.profile.expiresAtMs?.let(RevisionFieldValue::EpochMillis),
        )
        addDifference(
            RevisionFieldId.Credential,
            historical.secret.credential.credential(),
            current.secret.credential.credential(),
        )
        addDifference(RevisionFieldId.Notes, historical.secret.notes.text(), current.secret.notes.text())
        addDifference(
            RevisionFieldId.CustomFields,
            historical.secret.customFields.takeIf { it.isNotEmpty() }
                ?.let(RevisionFieldValue::CustomFields),
            current.secret.customFields.takeIf { it.isNotEmpty() }
                ?.let(RevisionFieldValue::CustomFields),
        )
        addDifference(
            RevisionFieldId.Links,
            historical.links.takeIf { it.isNotEmpty() }?.let(RevisionFieldValue::Links),
            current.links.takeIf { it.isNotEmpty() }?.let(RevisionFieldValue::Links),
        )
        addDifference(
            RevisionFieldId.Attachments,
            historical.attachmentIds.takeIf { it.isNotEmpty() }
                ?.let(RevisionFieldValue::AttachmentIds),
            current.attachmentIds.takeIf { it.isNotEmpty() }
                ?.let(RevisionFieldValue::AttachmentIds),
        )

        (historical.sensitiveFieldKeys + current.sensitiveFieldKeys).forEach { key ->
            val kind = when {
                key !in historical.sensitiveFieldKeys -> RevisionDifferenceKind.ADDED
                key !in current.sensitiveFieldKeys -> RevisionDifferenceKind.REMOVED
                else -> RevisionDifferenceKind.UNCHANGED
            }
            add(EntryRevisionDifference(RevisionFieldId.Sensitive(key), kind, null, null))
        }
    }

    private fun MutableList<EntryRevisionDifference>.addDifference(
        field: RevisionFieldId,
        before: RevisionFieldValue?,
        after: RevisionFieldValue?,
    ) {
        add(valueDifference(field, before, after))
    }

    private fun valueDifference(
        field: RevisionFieldId,
        before: RevisionFieldValue?,
        after: RevisionFieldValue?,
    ) = EntryRevisionDifference(
        field = field,
        kind = when {
            before == null && after != null -> RevisionDifferenceKind.ADDED
            before != null && after == null -> RevisionDifferenceKind.REMOVED
            before == after -> RevisionDifferenceKind.UNCHANGED
            else -> RevisionDifferenceKind.CHANGED
        },
        before = before,
        after = after,
    )

    private fun String?.text() = this?.takeIf(String::isNotBlank)?.let(RevisionFieldValue::Text)
    private fun Set<String>.textSet() = takeIf { it.isNotEmpty() }?.let(RevisionFieldValue::TextSet)
    private fun EntryCredential.credential() = RevisionFieldValue.Credential(this)
}
