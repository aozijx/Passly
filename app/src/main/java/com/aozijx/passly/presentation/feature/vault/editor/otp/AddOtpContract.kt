package com.aozijx.passly.presentation.feature.vault.editor.otp

sealed interface AddOtpEvent {
    data object UriParsed : AddOtpEvent
    data object UriParseFailed : AddOtpEvent
}
