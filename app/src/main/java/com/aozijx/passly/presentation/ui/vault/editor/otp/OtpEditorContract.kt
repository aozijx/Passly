package com.aozijx.passly.presentation.ui.vault.editor.otp

import androidx.compose.runtime.Immutable

enum class OtpEditorType { TOTP, HOTP, STEAM }

enum class OtpEditorAlgorithm { SHA1, SHA256, SHA512 }

enum class OtpEditorEncoding { BASE32, BASE64 }

@Immutable
class OtpEditorState(
    val title: String,
    val issuer: String,
    val accountName: String,
    val secret: String,
    val period: String,
    val digits: String,
    val type: OtpEditorType,
    val algorithm: OtpEditorAlgorithm,
    val encoding: OtpEditorEncoding,
    val counter: String,
    val uriText: String,
    val canSave: Boolean,
    val isSaving: Boolean,
)

data class OtpEditorEventHandler(
    val onBack: () -> Unit,
    val onSave: () -> Unit,
    val onScan: () -> Unit,
    val onTitleChange: (String) -> Unit,
    val onUriChange: (String) -> Unit,
    val onIssuerChange: (String) -> Unit,
    val onAccountNameChange: (String) -> Unit,
    val onSecretChange: (String) -> Unit,
    val onPeriodChange: (String) -> Unit,
    val onDigitsChange: (String) -> Unit,
    val onTypeChange: (OtpEditorType) -> Unit,
    val onAlgorithmChange: (OtpEditorAlgorithm) -> Unit,
    val onEncodingChange: (OtpEditorEncoding) -> Unit,
    val onCounterChange: (String) -> Unit,
)
