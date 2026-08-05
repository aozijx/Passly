package com.aozijx.passly.feature.recovery.contract

data class RecoveryModeUiState(
    val showSetPasswordDialog: Boolean = false,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSettingPassword: Boolean = false,
    val passwordSetupError: String? = null,

    val biometricResult: Boolean? = null,
    val isReconfiguringBiometric: Boolean = false,

    val showExportOptions: Boolean = false,
    val exportPassword: String = "",
    val includeIcons: Boolean = true,
    val includeAttachments: Boolean = true,
    val includeDeleted: Boolean = true,
    val exportError: String? = null,
    val isExporting: Boolean = false,
) {
    val canSubmitExport: Boolean
        get() = exportPassword.isNotBlank()
}