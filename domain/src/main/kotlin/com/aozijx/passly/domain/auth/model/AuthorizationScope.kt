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

    data class Global(
        override val purpose: AuthenticationPurpose,
    ) : AuthorizationScope
}
