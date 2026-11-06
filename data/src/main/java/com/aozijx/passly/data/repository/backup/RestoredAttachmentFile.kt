package com.aozijx.passly.data.repository.backup

import com.aozijx.passly.core.crypto.AttachmentContentProtector
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException

internal suspend fun retainRestoredAttachmentFile(
    target: File,
    content: ByteArray,
    resourceId: String,
    attachmentContentProtector: AttachmentContentProtector,
    fileJournal: VaultRestoreFileJournal,
    retainedCanonicalPaths: MutableSet<String>,
) {
    require(!target.exists() || target.isFile) { "附件恢复目标不是文件: ${target.name}" }
    val existingFileIsValid =
        target.isFile && existingAttachmentMatches(target, content, resourceId, attachmentContentProtector)
    if (!existingFileIsValid) {
        val encryptedContent = attachmentContentProtector.encrypt(content, resourceId)
        try {
            fileJournal.replace(target, encryptedContent)
        } finally {
            encryptedContent.fill(0)
        }
    }
    require(target.isFile) { "附件恢复文件缺失: ${target.name}" }
    retainedCanonicalPaths += target.canonicalPath
}

private suspend fun existingAttachmentMatches(
    target: File,
    content: ByteArray,
    resourceId: String,
    attachmentContentProtector: AttachmentContentProtector,
): Boolean {
    val encryptedContent =
        try {
            target.readBytes()
        } catch (_: IOException) {
            return false
        }
    val restoredContent =
        try {
            attachmentContentProtector.decrypt(encryptedContent, resourceId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return false
        } finally {
            encryptedContent.fill(0)
        }
    return try {
        restoredContent.size == content.size &&
            restoredContent.contentEquals(content) &&
            attachmentContentProtector.verifyContentId(restoredContent, resourceId)
    } finally {
        restoredContent.fill(0)
    }
}
