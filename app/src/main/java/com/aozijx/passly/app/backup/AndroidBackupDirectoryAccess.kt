package com.aozijx.passly.app.backup

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.feature.settings.backup.BackupDirectoryAccess
import com.aozijx.passly.feature.settings.backup.BackupDirectoryGrantResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidBackupDirectoryAccess @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BackupDirectoryAccess {
    override suspend fun acquire(treeUri: String): BackupDirectoryGrantResult =
        withContext(Dispatchers.IO) {
            val uri = treeUri.toUri()
            try {
                context.contentResolver.takePersistableUriPermission(uri, PERMISSION_FLAGS)
            } catch (_: SecurityException) {
                return@withContext BackupDirectoryGrantResult.PermissionDenied
            }

            BackupStorageSupport.ensureAppDirectoryTreeUri(context, uri).fold(
                onSuccess = { BackupDirectoryGrantResult.Granted(treeUri) },
                onFailure = {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            uri,
                            PERMISSION_FLAGS,
                        )
                    }
                    BackupDirectoryGrantResult.Unavailable
                },
            )
        }

    override suspend fun release(treeUri: String) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                treeUri.toUri(),
                PERMISSION_FLAGS,
            )
        }
        Unit
    }

    private companion object {
        const val PERMISSION_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
