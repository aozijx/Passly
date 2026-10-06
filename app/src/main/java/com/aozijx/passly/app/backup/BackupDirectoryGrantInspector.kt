package com.aozijx.passly.app.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.core.platform.storage.resolvePersistedDirectoryAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupDirectoryGrantInspector @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun inspect(treeUri: String?): PersistedDirectoryAccessStatus {
        val configuredUri = treeUri?.takeUnless(String::isBlank)
        if (configuredUri == null) {
            return resolvePersistedDirectoryAccess(
                configured = false,
                uriValid = false,
                hasWriteGrant = false,
                providerAvailable = false,
            )
        }

        val uri = configuredUri.toUri()
        if (!uri.isValidTreeUri()) {
            return resolvePersistedDirectoryAccess(
                configured = true,
                uriValid = false,
                hasWriteGrant = false,
                providerAvailable = false,
            )
        }

        val hasWriteGrant = context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
        return resolvePersistedDirectoryAccess(
            configured = true,
            uriValid = true,
            hasWriteGrant = hasWriteGrant,
            providerAvailable = providerAndTreeAreAvailable(uri, hasWriteGrant),
        )
    }

    private fun providerAndTreeAreAvailable(uri: Uri, hasWriteGrant: Boolean): Boolean {
        val authority = uri.authority ?: return false
        val provider = runCatching {
            context.contentResolver.acquireUnstableContentProviderClient(authority)
        }.getOrNull() ?: return false
        provider.close()
        if (!hasWriteGrant) return true

        return runCatching {
            val documentId = DocumentsContract.getTreeDocumentId(uri)
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
            context.contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            )?.use { cursor -> cursor.moveToFirst() } == true
        }.getOrDefault(false)
    }

    private fun Uri.isValidTreeUri(): Boolean =
        scheme == "content" &&
            !authority.isNullOrBlank() &&
            runCatching { DocumentsContract.isTreeUri(this) }.getOrDefault(false)
}
