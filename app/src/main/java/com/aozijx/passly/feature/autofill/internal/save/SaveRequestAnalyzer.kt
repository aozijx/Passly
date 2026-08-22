package com.aozijx.passly.feature.autofill.internal.save

import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import javax.inject.Inject
import javax.inject.Singleton

data class PendingSaveCredential(
    val packageName: String?,
    val webDomain: String?,
    val pageTitle: String?,
    val username: String,
    val password: String,
)

@Singleton
class SaveRequestAnalyzer @Inject constructor() {

    fun buildCandidate(
        parsed: ParsedStructure,
        request: AutofillRequest,
        strategy: FieldMatchStrategy,
        settings: AutofillSettings,
    ): PendingSaveCredential? {
        if (!settings.enabled || !settings.savePromptsEnabled) return null

        val matchResult = strategy.match(request)
        val usernameField = parsed.editableFields.find {
            matchResult.roleMap[it.autofillId.toString()] == FieldRole.USERNAME
        }
        val passwordField = parsed.editableFields.find {
            matchResult.roleMap[it.autofillId.toString()] == FieldRole.PASSWORD
        }

        var usernameValue = usernameField?.value?.takeIf(String::isNotBlank)
        var passwordValue = passwordField?.value?.takeIf(String::isNotBlank)

        if (usernameValue == null || passwordValue == null) {
            val candidates = parsed.editableFields.filter { !it.value.isNullOrBlank() }
            if (passwordValue == null) {
                passwordValue = candidates.lastOrNull {
                    it.inputType?.contains("PASSWORD") == true ||
                        it.className?.contains("password", ignoreCase = true) == true
                }?.value
            }
            if (usernameValue == null) {
                usernameValue = candidates.firstOrNull { it.value != passwordValue }?.value
            }
        }

        val username = usernameValue?.takeIf(String::isNotBlank) ?: return null
        val password = passwordValue?.takeIf(String::isNotBlank) ?: return null
        return PendingSaveCredential(
            packageName = parsed.packageName,
            webDomain = parsed.webDomain,
            pageTitle = parsed.pageTitle,
            username = username,
            password = password,
        )
    }
}
