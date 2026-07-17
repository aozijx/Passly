package com.aozijx.passly.feature.main.contract

import java.io.File

data class MainUiState(
    val isAuthorized: Boolean = false,
    val isDarkMode: Boolean? = null,
    val isDynamicColor: Boolean = true,
    val themeColor: Long = 0,
    val isDatabaseInitializing: Boolean = false,
    val databaseError: Throwable? = null,
    val emergencyBackupFile: File? = null,
    val plainBackupFile: File? = null
)
