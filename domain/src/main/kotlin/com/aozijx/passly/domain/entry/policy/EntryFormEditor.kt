package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryType

/** Applies one complete supported editor form while preserving entry state the form does not own. */
class EntryFormEditor(
    private val materializer: EntryDraftMaterializer = EntryDraftMaterializer(),
) {
    fun apply(entry: Entry, draft: EntryDraft): EntryEditResult {
        if (entry.type != draft.type || draft.type !in supportedTypes) return EntryEditResult.Unsupported

        val replacement = try {
            materializer.materialize(draft, entry.identity)
        } catch (invalid: InvalidEntryDraft) {
            val required = draft.missingRequiredFields(EntryTypeDefinitions[draft.type])
            return EntryEditResult.Invalid(
                invalid.fields.mapTo(linkedSetOf()) { field ->
                    EntryViolation(
                        field = field,
                        code = if (field in required) EntryViolationCode.REQUIRED
                        else EntryViolationCode.INVALID_FORMAT,
                    )
                },
            )
        }

        val profile = replacement.profile.copy(
            username = if (draft.type == EntryType.LOGIN) replacement.profile.username else entry.profile.username,
            associations = if (draft.type == EntryType.LOGIN) {
                replacement.profile.associations.copy(domains = entry.profile.associations.domains)
            } else {
                entry.profile.associations
            },
            icon = entry.profile.icon,
            favorite = entry.profile.favorite,
            expiresAtMs = entry.profile.expiresAtMs,
        )
        val updated = replacement.copy(
            profile = profile,
            secret = replacement.secret.copy(customFields = entry.secret.customFields),
        )
        val violations = EntryValidation.validate(updated)
        return if (violations.isEmpty()) EntryEditResult.Updated(updated) else EntryEditResult.Invalid(violations)
    }

    private companion object {
        val supportedTypes = setOf(EntryType.LOGIN, EntryType.OTP, EntryType.BANK_CARD, EntryType.WIFI)
    }
}
