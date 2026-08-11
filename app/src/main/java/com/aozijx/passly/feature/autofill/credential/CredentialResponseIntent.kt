package com.aozijx.passly.feature.autofill.credential

import android.content.Intent

sealed interface CredentialResponseIntent {
    data class PasswordGet(val sourceIntent: Intent) : CredentialResponseIntent
    data class Unlock(val sourceIntent: Intent) : CredentialResponseIntent
    data class PasswordCreate(val sourceIntent: Intent) : CredentialResponseIntent
    data object UnknownAction : CredentialResponseIntent
}
