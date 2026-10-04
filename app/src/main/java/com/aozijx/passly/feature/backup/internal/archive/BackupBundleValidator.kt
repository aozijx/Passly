package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import java.security.MessageDigest

object BackupBundleValidator {
    const val MAX_RESOURCE_BYTES = 16 * 1024 * 1024
    const val MAX_TOTAL_RESOURCE_BYTES = 128 * 1024 * 1024
    const val MAX_ENTRIES = 100_000
    const val MAX_RESOURCES = 100_000
    fun validate(bundle: BackupBundle, requireResourceData: Boolean) {
        KeyedBackupBundleValidator.validate(bundle, requireResourceData)
    }

    fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(data)
            .joinToString("") { "%02x".format(it) }

}
