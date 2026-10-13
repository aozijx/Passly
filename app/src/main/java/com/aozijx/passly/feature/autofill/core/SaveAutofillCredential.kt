package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.autofill.model.AutofillOperationFailure
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.model.AutofillSaveSource
import com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class SaveAutofillCredential @Inject constructor(
    private val settingsSource: InteractionSettingsSource,
    private val applicationLabelResolver: ApplicationLabelResolver,
    private val createEntry: CreateEntryUseCase,
) {
    suspend operator fun invoke(
        draft: AutofillSaveDraft,
        source: AutofillSaveSource,
    ): AutofillSaveOutcome {
        val policy = settingsSource.interaction.first().autofill
        val sourceEnabled = when (source) {
            AutofillSaveSource.LEGACY_PROMPT -> policy.savePromptsEnabled
            AutofillSaveSource.CREDENTIAL_MANAGER -> policy.credentialManagerEnabled
        }
        if (!policy.enabled || !sourceEnabled) return AutofillSaveOutcome.Disabled
        if (draft.password.isBlank()) return AutofillSaveOutcome.InvalidDraft

        return when (
            val result = createEntry.createFromAutofill(
                buildAutofillCredentialDraft(draft, applicationLabelResolver),
            )
        ) {
            is AppResult.Success -> AutofillSaveOutcome.Saved(result.data)
            is AppResult.Failure -> AutofillSaveOutcome.Failed(
                AutofillOperationFailure.PERSISTENCE_FAILED,
            )
        }
    }

}

internal fun buildAutofillCredentialDraft(
    draft: AutofillSaveDraft,
    applicationLabelResolver: ApplicationLabelResolver,
): EntryDraft {
    val applicationId = AutofillScope.normalizeApplicationId(draft.packageName)
    val domain = AutofillScope.normalizeDomain(draft.webDomain)
    val title = resolveAutofillCredentialTitle(
        applicationId = applicationId,
        appLabel = applicationId?.let(applicationLabelResolver::labelFor),
        domain = domain,
        pageTitle = draft.pageTitle,
        usernameValue = draft.username,
    )
    val definition = EntryTypeDefinitions[EntryType.LOGIN]
    var entryDraft = EntryDraft(EntryDraftTarget.New(EntryType.LOGIN))
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title))
        .withValue(definition, FieldKey.PASSWORD, EntryDraftValue.Text(draft.password))
    draft.username.trim().takeIf(String::isNotEmpty)?.let {
        entryDraft = entryDraft.withValue(definition, FieldKey.USERNAME, EntryDraftValue.Text(it))
    }
    draft.webDomain?.trim()?.takeIf(String::isNotEmpty)?.let {
        entryDraft = entryDraft.withValue(definition, FieldKey.PRIMARY_URL, EntryDraftValue.Text(it))
    }
    domain?.let {
        entryDraft = entryDraft.withValue(
            definition,
            FieldKey.DOMAINS,
            EntryDraftValue.TextList(listOf(it)),
        )
    }
    applicationId?.let {
        entryDraft = entryDraft.withValue(
            definition,
            FieldKey.APPLICATION_IDS,
            EntryDraftValue.TextList(listOf(it)),
        )
    }
    return entryDraft
}

internal fun resolveAutofillCredentialTitle(
    applicationId: String?,
    appLabel: String?,
    domain: String?,
    pageTitle: String?,
    usernameValue: String,
): String {
    fun String.isApplicationIdTitle(): Boolean = applicationId != null &&
        (equals(applicationId, ignoreCase = true) || startsWith("$applicationId/", ignoreCase = true))

    val normalizedAppLabel = appLabel?.trim()
        ?.takeIf(String::isNotBlank)
        ?.takeUnless(String::isApplicationIdTitle)
    val normalizedPageTitle = pageTitle?.trim()
        ?.takeIf { it.any(Char::isLetter) }
        ?.takeUnless(String::isApplicationIdTitle)

    return if (domain != null) {
        normalizedPageTitle ?: domain
    } else {
        normalizedAppLabel
            ?: normalizedPageTitle
            ?: usernameValue.trim().takeIf(String::isNotBlank)
            ?: "Login"
    }
}
