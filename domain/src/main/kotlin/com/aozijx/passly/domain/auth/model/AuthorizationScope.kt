package com.aozijx.passly.domain.auth.model

import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.SensitiveAccessAction
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

sealed interface AuthorizationScope {
    val purpose: AuthenticationPurpose

    data class SensitiveFields(
        val entryId: EntryId,
        val fieldKeys: Set<SensitiveFieldKey>,
        val action: SensitiveAccessAction,
        override val purpose: AuthenticationPurpose =
            AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET,
    ) : AuthorizationScope {
        init {
            require(fieldKeys.isNotEmpty()) { "Sensitive-field authorization scope cannot be empty" }
        }
    }

    data class SensitiveRevision(
        val entryId: EntryId,
        val revisionId: String,
        val fieldKeys: Set<SensitiveFieldKey>,
        val action: SensitiveRevisionAccessAction,
        override val purpose: AuthenticationPurpose =
            AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET,
    ) : AuthorizationScope {
        init {
            require(revisionId.isNotBlank()) { "Sensitive revision ID cannot be blank" }
            require(fieldKeys.isNotEmpty()) { "Sensitive revision scope cannot be empty" }
        }
    }

    data class Global(
        override val purpose: AuthenticationPurpose,
    ) : AuthorizationScope
}

enum class SensitiveRevisionAccessAction {
    REVEAL,
    RESTORE,
}
