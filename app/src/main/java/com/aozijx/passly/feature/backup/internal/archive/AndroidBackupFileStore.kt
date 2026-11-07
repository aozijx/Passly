package com.aozijx.passly.feature.backup.internal.archive

import android.content.Context
import androidx.core.net.toUri
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.feature.backup.internal.archive.io.BackupFileStore
import com.aozijx.passly.feature.backup.internal.archive.io.LimitedInputStream
import com.aozijx.passly.feature.backup.internal.archive.io.LimitedOutputStream
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidBackupFileStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : BackupFileStore {
        private fun openInputStream(uri: String): InputStream {
            val parsed = uri.toUri()
            return try {
                context.contentResolver.openInputStream(parsed)
                    ?: throw BackupFailed()
            } catch (_: SecurityException) {
                throw BackupFailed()
            } catch (_: FileNotFoundException) {
                throw BackupFailed()
            }
        }

        private fun openOutputStream(uri: String): OutputStream {
            val parsed = uri.toUri()
            return try {
                context.contentResolver.openOutputStream(parsed, "rwt")
                    ?: throw BackupFailed()
            } catch (_: SecurityException) {
                throw BackupFailed()
            } catch (_: FileNotFoundException) {
                throw BackupFailed()
            }
        }

        override fun openOutput(
            uri: String,
            maxBytes: Long,
        ): OutputStream = LimitedOutputStream(openOutputStream(uri), maxBytes)

        override fun openInput(
            uri: String,
            maxBytes: Long,
        ): InputStream = LimitedInputStream(openInputStream(uri), maxBytes)
    }
