package com.aozijx.passly.features.main.contract

import java.io.File

data class MainUiState(
    val isAuthorized: Boolean = false,
    val isDarkMode: Boolean? = null,
    val isDynamicColor: Boolean = true,
    val lockTimeoutMs: Long = 60_000L,
    val isDatabaseInitializing: Boolean = true,
    val databaseError: Throwable? = null,
    val validationMessage: String? = null,
    val emergencyBackupFile: File? = null,
    val plainBackupFile: File? = null
)
