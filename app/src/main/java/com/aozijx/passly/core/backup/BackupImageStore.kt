package com.aozijx.passly.core.backup

import java.io.File
import java.io.InputStream

interface BackupImageStore {
    fun resolveReadable(path: String?): File?
    fun saveFromBackup(fileName: String, input: InputStream): String?
}

