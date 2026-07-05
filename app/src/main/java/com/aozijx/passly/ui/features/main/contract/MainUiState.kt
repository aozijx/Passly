package com.aozijx.passly.ui.features.main.contract

import com.aozijx.passly.domain.model.AppDefaults
import java.io.File

data class MainUiState(
    val isAuthorized: Boolean = false,
    val isDarkMode: Boolean? = null,
    val isDynamicColor: Boolean = AppDefaults.Display.DYNAMIC_COLOR,
    val themeColor: Long = 0,
    val isDatabaseInitializing: Boolean = false,
    val databaseError: Throwable? = null,
    val validationMessage: String? = null,
    val emergencyBackupFile: File? = null,
    val plainBackupFile: File? = null
)