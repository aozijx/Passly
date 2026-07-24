package com.aozijx.passly.domain.model.backup

/**
 * Stable identifier for a backup format.
 *
 * This is deliberately not an enum: third-party import adapters can introduce
 * formats without changing the backup service contract.
 */
@JvmInline
value class BackupFormatId(val value: String) {
    init {
        require(value.matches(Regex("[a-z0-9][a-z0-9._-]{1,63}"))) {
            "Invalid backup format id: $value"
        }
    }
}

object BackupFormats {
    val PASSLY_ENCRYPTED = BackupFormatId("passly.encrypted")
    val PASSLY_JSON = BackupFormatId("passly.json")
    val READABLE_TEXT = BackupFormatId("passly.text")
    val BITWARDEN_JSON = BackupFormatId("bitwarden.json")
}
