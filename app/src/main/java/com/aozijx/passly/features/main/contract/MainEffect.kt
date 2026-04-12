package com.aozijx.passly.features.main.contract

sealed interface MainEffect {
    data class ShowToast(val message: String) : MainEffect
    data class ShowError(val error: String) : MainEffect
    data object LockedByTimeout : MainEffect
    data object NavigateToVault : MainEffect

    /** 请 UI 弹出「创建文件」选择器，并用返回的 URI 触发 [com.aozijx.passly.features.main.contract.MainIntent.ExportPlainBackupToUri]。 */
    data class ShowPlainExportPicker(val fileName: String) : MainEffect
}
