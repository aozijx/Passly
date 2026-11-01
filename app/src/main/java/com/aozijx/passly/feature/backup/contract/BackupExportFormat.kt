package com.aozijx.passly.feature.backup.contract

/** Export choices shared by Backup presentation and its execution boundary. */
enum class BackupExportFormat(
    val extension: String,
    val mimeType: String,
    val requiresPassword: Boolean,
    val supportsResources: Boolean,
) {
    ENCRYPTED("passly", "application/octet-stream", requiresPassword = true, supportsResources = true),
    JSON("json", "application/json", requiresPassword = false, supportsResources = true),
    TEXT("txt", "text/plain", requiresPassword = false, supportsResources = false),
}
