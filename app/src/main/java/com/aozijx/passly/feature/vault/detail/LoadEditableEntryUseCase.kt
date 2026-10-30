package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.SensitiveValue
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

internal class LoadEditableEntryUseCase @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val authorizationGate: AuthorizationGate,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val activityRecorder: ActivityRecorder,
) {
    suspend operator fun invoke(entryId: EntryId): EditableEntryLoadResult {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) return EditableEntryLoadResult.Unavailable
        val initialAuthentication = secureSessionAccessState.authenticationState.value
            as? AuthenticationState.Authenticated
            ?: return EditableEntryLoadResult.Unavailable
        val entry = entryQueryRepository.getById(entryId) ?: return EditableEntryLoadResult.NotFound
        if (entry.id != entryId) return EditableEntryLoadResult.Unavailable
        val type = entry.type
        if (type !in EDITABLE_TYPES) return EditableEntryLoadResult.UnsupportedType
        val presence = sensitiveFieldRepository.getPresence(entryId)
        if (!hasCurrentAccess(initialAuthentication)) return EditableEntryLoadResult.Unavailable

        val fieldsBySensitiveKey = EntryTypeDefinitions[type].fields.mapNotNull { field ->
            field.sensitiveFieldKey?.let { it to field.key }
        }.toMap()
        if (presence.keys.any { it !in fieldsBySensitiveKey }) return EditableEntryLoadResult.Unavailable
        val requestedKeys = presence.keys
        if (requestedKeys.isEmpty()) {
            val latest = entryQueryRepository.getById(entryId) ?: return EditableEntryLoadResult.NotFound
            return if (latest.version == entry.version && latest.type == type && hasCurrentAccess(initialAuthentication)) {
                EditableEntryLoadResult.Loaded(entry, initialAuthentication)
            } else {
                EditableEntryLoadResult.Unavailable
            }
        }

        val scope = AuthorizationScope.SensitiveFields(
            entryId = entryId,
            fieldKeys = requestedKeys,
            action = SensitiveAccessAction.REVEAL,
        )
        var authorizedAuthentication: AuthenticationState.Authenticated? = null
        val authorization = authorizationGate.authorize(scope) { permit ->
            val currentAuthentication = secureSessionAccessState.authenticationState.value
                as? AuthenticationState.Authenticated
                ?: return@authorize null
            if (!secureSessionAccessState.hasFullSecureSessionAccess()) return@authorize null
            authorizedAuthentication = currentAuthentication
            val revealed = sensitiveFieldRepository.revealMany(
                entryId = entryId,
                keys = requestedKeys,
                action = SensitiveAccessAction.REVEAL,
                permit = permit,
            )
            try {
                if (revealed.size != requestedKeys.size || revealed.any { it.entryId != entryId }) {
                    return@authorize null
                }
                var editable = entry
                val seen = mutableSetOf<SensitiveFieldKey>()
                for (field in revealed) {
                    if (!seen.add(field.key)) return@authorize null
                    val fieldKey = fieldsBySensitiveKey[field.key] ?: return@authorize null
                    val value = field.value.toTransientString()
                    editable = editable.withRevealedField(fieldKey, value)
                        ?: return@authorize null
                }
                if (seen != requestedKeys || !hasCurrentAccess(currentAuthentication)) return@authorize null
                editable
            } finally {
                revealed.forEach { it.value.wipe() }
            }
        }
        return when (authorization) {
            is AuthorizationResult.Allowed -> {
                val editable = authorization.value ?: return EditableEntryLoadResult.Unavailable
                val currentAuthentication = authorizedAuthentication
                    ?: return EditableEntryLoadResult.Unavailable
                val latest = entryQueryRepository.getById(entryId)
                    ?: return EditableEntryLoadResult.NotFound
                if (latest.version != entry.version || latest.type != type ||
                    !hasCurrentAccess(currentAuthentication)
                ) return EditableEntryLoadResult.Unavailable
                activityRecorder.recordUsage(entryId.value, ActivityType.VIEW)
                if (!hasCurrentAccess(currentAuthentication)) EditableEntryLoadResult.Unavailable
                else EditableEntryLoadResult.Loaded(editable, currentAuthentication)
            }
            is AuthorizationResult.Denied -> EditableEntryLoadResult.AccessDenied
            AuthorizationResult.Cancelled -> EditableEntryLoadResult.Cancelled
        }
    }

    private fun hasCurrentAccess(authentication: AuthenticationState.Authenticated): Boolean =
        secureSessionAccessState.hasFullSecureSessionAccess() &&
            secureSessionAccessState.authenticationState.value === authentication

    private fun Entry.withRevealedField(field: FieldKey, value: String): Entry? = when (type) {
        EntryType.LOGIN -> if (field == FieldKey.PASSWORD) {
            val credential = secret.login ?: return null
            copy(secret = secret.copy(credential = credential.copy(password = value)))
        } else null
        EntryType.BANK_CARD -> {
            val credential = secret.card ?: return null
            val updated = when (field) {
                FieldKey.CARD_NUMBER -> credential.copy(cardNumber = value)
                FieldKey.CARD_CVV -> credential.copy(cardCvv = value)
                FieldKey.PAYMENT_PIN -> credential.copy(paymentPin = value)
                else -> return null
            }
            copy(secret = secret.copy(credential = updated))
        }
        EntryType.OTP -> if (field == FieldKey.OTP_SECRET) {
            val credential = secret.otp ?: return null
            copy(secret = secret.copy(credential = OtpCredential(credential.config.copy(secret = value))))
        } else null
        EntryType.WIFI -> if (field == FieldKey.PASSWORD) {
            val credential = secret.wifi ?: return null
            copy(secret = secret.copy(credential = credential.copy(password = value)))
        } else null
        else -> null
    }

    private fun SensitiveValue.toTransientString(): String {
        val chars = toCharArray()
        return try {
            String(chars)
        } finally {
            chars.fill('\u0000')
        }
    }

    private companion object {
        val EDITABLE_TYPES = setOf(EntryType.LOGIN, EntryType.OTP, EntryType.BANK_CARD, EntryType.WIFI)
    }
}

internal sealed interface EditableEntryLoadResult {
    data class Loaded(val entry: Entry, val authentication: AuthenticationState.Authenticated) : EditableEntryLoadResult
    data object NotFound : EditableEntryLoadResult
    data object UnsupportedType : EditableEntryLoadResult
    data object AccessDenied : EditableEntryLoadResult
    data object Cancelled : EditableEntryLoadResult
    data object Unavailable : EditableEntryLoadResult
}
