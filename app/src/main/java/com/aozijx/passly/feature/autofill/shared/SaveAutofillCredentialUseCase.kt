package com.aozijx.passly.feature.autofill.shared

import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveAutofillCredentialUseCase @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val applicationLabelResolver: ApplicationLabelResolver,
    private val createEntry: CreateEntryUseCase,
) {
    suspend operator fun invoke(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String,
        source: AutofillSaveSource = AutofillSaveSource.LEGACY_PROMPT,
    ): AppResult<EntryId> {
        val policy = settingsRepository.settings.first().interaction.autofill
        val sourceEnabled = when (source) {
            AutofillSaveSource.LEGACY_PROMPT -> policy.savePromptsEnabled
            AutofillSaveSource.CREDENTIAL_MANAGER -> policy.credentialManagerEnabled
        }
        if (!policy.enabled || !sourceEnabled || passwordValue.isBlank()) {
            return AppResult.Failure(ValidationError())
        }

        return createEntry(
            buildAutofillCredentialDraft(
                packageName = packageName,
                webDomain = webDomain,
                pageTitle = pageTitle,
                usernameValue = usernameValue,
                passwordValue = passwordValue,
                applicationLabelResolver = applicationLabelResolver,
            ),
        )
    }
}

enum class AutofillSaveSource {
    LEGACY_PROMPT,
    CREDENTIAL_MANAGER,
}

internal fun buildAutofillCredentialDraft(
    packageName: String?,
    webDomain: String?,
    pageTitle: String?,
    usernameValue: String,
    passwordValue: String,
    applicationLabelResolver: ApplicationLabelResolver,
): EntryDraft {
    val applicationId = AutofillScope.normalizeApplicationId(packageName)
    val domain = AutofillScope.normalizeDomain(webDomain)
    val title = resolveAutofillCredentialTitle(
        applicationId = applicationId,
        appLabel = applicationId?.let(applicationLabelResolver::labelFor),
        domain = domain,
        pageTitle = pageTitle,
        usernameValue = usernameValue,
    )
    val definition = EntryTypeDefinitions[EntryType.LOGIN]
    var draft = EntryDraft(EntryDraftTarget.New(EntryType.LOGIN))
        .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text(title))
        .withValue(definition, FieldKey.PASSWORD, EntryDraftValue.Text(passwordValue))
    usernameValue.trim().takeIf(String::isNotEmpty)?.let {
        draft = draft.withValue(definition, FieldKey.USERNAME, EntryDraftValue.Text(it))
    }
    webDomain?.trim()?.takeIf(String::isNotEmpty)?.let {
        draft = draft.withValue(definition, FieldKey.PRIMARY_URL, EntryDraftValue.Text(it))
    }
    domain?.let {
        draft = draft.withValue(definition, FieldKey.DOMAINS, EntryDraftValue.TextList(listOf(it)))
    }
    applicationId?.let {
        draft = draft.withValue(
            definition,
            FieldKey.APPLICATION_IDS,
            EntryDraftValue.TextList(listOf(it)),
        )
    }
    return draft
}

internal fun resolveAutofillCredentialTitle(
    applicationId: String?,
    appLabel: String?,
    domain: String?,
    pageTitle: String?,
    usernameValue: String,
): String {
    fun String.isApplicationIdTitle(): Boolean = applicationId != null &&
        (equals(applicationId, ignoreCase = true) ||
            startsWith("$applicationId/", ignoreCase = true))

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
