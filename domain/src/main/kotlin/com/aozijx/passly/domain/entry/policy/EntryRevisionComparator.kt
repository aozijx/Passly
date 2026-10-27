package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryFieldAccess
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CustomFieldKind
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
object EntryRevisionComparator {
    private val fieldReader = DefaultEntryFieldReader()
    private val aggregateFields = setOf(
        FieldKey.TITLE, FieldKey.USERNAME, FieldKey.PRIMARY_URL, FieldKey.DOMAINS,
        FieldKey.APPLICATION_IDS, FieldKey.FAVORITE, FieldKey.TAGS, FieldKey.EXPIRES_AT,
        FieldKey.NOTES, FieldKey.CUSTOM_FIELDS,
    )
    fun compare(
        historical: RedactedEntryRevision,
        current: RedactedEntryRevision,
    ): List<EntryRevisionDifference> = buildList {
        require(historical.metadata.entryId == current.metadata.entryId && historical.type == current.type) {
            "Revision comparison requires the same entry and type"
        }
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
        val definitions = EntryTypeDefinitions[current.type].fields
        definitions.filter { it.key !in aggregateFields && it.access != EntryFieldAccess.HIGH_SENSITIVITY }
            .forEach { field ->
                val before = fieldReader.getFieldValue(historical.profile, historical.secret, field.key)
                val after = fieldReader.getFieldValue(current.profile, current.secret, field.key)
                if (field.key == FieldKey.PASSWORD || field.key == FieldKey.SECURITY_ANSWER) {
                    add(EntryRevisionDifference(RevisionFieldId.ProtectedField(field.key), differenceKind(before, after), null, null))
                } else {
                    addDifference(RevisionFieldId.CredentialField(field.key), before.publicValue(field.valueType), after.publicValue(field.valueType))
                }
            }
        addDifference(RevisionFieldId.Notes, historical.secret.notes.text(), current.secret.notes.text())
        repeat(maxOf(historical.secret.customFields.size, current.secret.customFields.size)) { position ->
            val before = historical.secret.customFields.getOrNull(position)
            val after = current.secret.customFields.getOrNull(position)
            val name = requireNotNull(after?.name ?: before?.name)
            if (before?.kind == CustomFieldKind.HIDDEN || after?.kind == CustomFieldKind.HIDDEN) {
                add(EntryRevisionDifference(RevisionFieldId.ProtectedCustomField(position, name), differenceKind(before, after), null, null))
            } else {
                add(EntryRevisionDifference(
                    RevisionFieldId.CustomField(position, name), differenceKind(before, after), before?.value.text(), after?.value.text(),
                ))
            }
        }
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
                historical.sensitiveFieldFingerprints[key] != null &&
                    current.sensitiveFieldFingerprints[key] != null &&
                    historical.sensitiveFieldFingerprints[key] !=
                    current.sensitiveFieldFingerprints[key] -> RevisionDifferenceKind.CHANGED
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
        kind = differenceKind(before, after),
        before = before,
        after = after,
    )

    private fun <T> differenceKind(before: T?, after: T?): RevisionDifferenceKind = when {
        before == null && after != null -> RevisionDifferenceKind.ADDED
        before != null && after == null -> RevisionDifferenceKind.REMOVED
        before == after -> RevisionDifferenceKind.UNCHANGED
        else -> RevisionDifferenceKind.CHANGED
    }

    private fun String?.text() = this?.let(RevisionFieldValue::Text)
    private fun String?.publicValue(type: EntryFieldValueType): RevisionFieldValue? = when (type) {
        EntryFieldValueType.BOOLEAN -> this?.toBooleanStrict()?.let(RevisionFieldValue::Flag)
        EntryFieldValueType.TEXT, EntryFieldValueType.INTEGER, EntryFieldValueType.LONG,
        EntryFieldValueType.TEXT_LIST -> text()
    }
    private fun Set<String>.textSet() = takeIf { it.isNotEmpty() }?.let(RevisionFieldValue::TextSet)
}
