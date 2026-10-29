package com.aozijx.passly.data.local.autofill

import android.content.Context
import com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId
import com.aozijx.passly.domain.autofill.model.AutofillInboxLimits
import com.aozijx.passly.domain.autofill.model.AutofillInboxStoredRecord
import com.aozijx.passly.domain.access.port.AutofillInboxRecordStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
internal class AutofillInboxRecordStoreImpl @Inject constructor(
    @ApplicationContext context: Context,
) : AutofillInboxRecordStore {
    private val manager = AutofillInboxFileManager(
        root = File(context.filesDir, ROOT_DIRECTORY),
        nowMs = System::currentTimeMillis,
    )

    override suspend fun enqueue(
        keyId: ByteArray,
        draftId: AutofillInboxDraftId,
        queuedAtMs: Long,
        encodedRecord: ByteArray,
    ): Boolean = withContext(Dispatchers.IO) {
        manager.enqueue(keyId, draftId, queuedAtMs, encodedRecord)
    }

    override suspend fun pending(keyId: ByteArray): List<AutofillInboxStoredRecord> =
        withContext(Dispatchers.IO) { manager.pending(keyId) }

    override suspend fun remove(keyId: ByteArray, draftId: AutofillInboxDraftId) {
        withContext(Dispatchers.IO) { manager.remove(keyId, draftId) }
    }

    override suspend fun quarantine(keyId: ByteArray, draftId: AutofillInboxDraftId) {
        withContext(Dispatchers.IO) { manager.quarantine(keyId, draftId) }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { manager.clear() }
    }

    private companion object {
        const val ROOT_DIRECTORY = "vault/autofill-inbox"
    }
}

internal class AutofillInboxFileManager(
    private val root: File,
    private val nowMs: () -> Long,
    private val random: SecureRandom = SecureRandom(),
) {
    @Synchronized
    fun enqueue(
        keyId: ByteArray,
        draftId: AutofillInboxDraftId,
        queuedAtMs: Long,
        encodedRecord: ByteArray,
    ): Boolean {
        require(queuedAtMs >= 0) { "Inbox queue time cannot be negative" }
        require(encodedRecord.size in 1..AutofillInboxLimits.MAX_RECORD_BYTES) {
            "Invalid Inbox record size"
        }
        val keyName = keyId.toHexKey()
        cleanup(keyName)
        val pendingDirectory = child(child(root, keyName), PENDING_DIRECTORY)
        ensureDirectory(pendingDirectory)
        val active = filesInAllKeys(PENDING_DIRECTORY)
        if (active.size >= AutofillInboxLimits.MAX_PENDING_RECORDS) return false
        if (active.sumOf(File::length) + encodedRecord.size > AutofillInboxLimits.MAX_ACTIVE_BYTES) {
            return false
        }
        if (allFiles().sumOf(File::length) + encodedRecord.size > AutofillInboxLimits.MAX_ROOT_BYTES) {
            return false
        }

        val destination = child(pendingDirectory, draftId.value)
        if (destination.exists()) return false
        val temporary = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX, pendingDirectory)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(encodedRecord)
                output.fd.sync()
            }
            if (!temporary.renameTo(destination)) {
                throw IOException("Unable to atomically enqueue Autofill Inbox record")
            }
            if (!destination.setLastModified(queuedAtMs)) {
                destination.delete()
                throw IOException("Unable to set Autofill Inbox queue time")
            }
            return true
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    @Synchronized
    fun pending(keyId: ByteArray): List<AutofillInboxStoredRecord> {
        val keyName = keyId.toHexKey()
        cleanup(keyName)
        val directory = child(child(root, keyName), PENDING_DIRECTORY)
        if (!directory.isDirectory) return emptyList()
        return listRecordFiles(directory)
            .sortedWith(compareBy<File> { it.lastModified() }.thenBy { it.name })
            .map { file ->
                val size = file.length()
                require(size in 1..AutofillInboxLimits.MAX_RECORD_BYTES.toLong()) {
                    "Invalid stored Inbox record size"
                }
                AutofillInboxStoredRecord(
                    draftId = AutofillInboxDraftId(file.name),
                    queuedAtMs = file.lastModified(),
                    encodedRecord = file.readBytes(),
                )
            }
    }

    @Synchronized
    fun remove(keyId: ByteArray, draftId: AutofillInboxDraftId) {
        val file = recordFile(keyId, PENDING_DIRECTORY, draftId)
        if (file.exists() && !file.delete()) throw IOException("Unable to remove Inbox record")
    }

    @Synchronized
    fun quarantine(keyId: ByteArray, draftId: AutofillInboxDraftId) {
        val keyName = keyId.toHexKey()
        cleanup(keyName)
        val source = recordFile(keyId, PENDING_DIRECTORY, draftId)
        if (!source.exists()) return
        val quarantined = filesInAllKeys(QUARANTINE_DIRECTORY)
        if (quarantined.size >= AutofillInboxLimits.MAX_QUARANTINED_RECORDS) {
            throw IOException("Autofill Inbox quarantine is full")
        }
        if (quarantined.sumOf(File::length) + source.length() > AutofillInboxLimits.MAX_QUARANTINE_BYTES) {
            throw IOException("Autofill Inbox quarantine is full")
        }
        if (allFiles().sumOf(File::length) > AutofillInboxLimits.MAX_ROOT_BYTES) {
            throw IOException("Autofill Inbox storage limit exceeded")
        }
        val destinationDirectory = child(child(root, keyName), QUARANTINE_DIRECTORY)
        ensureDirectory(destinationDirectory)
        val destination = child(destinationDirectory, draftId.value)
        if (destination.exists() || !source.renameTo(destination)) {
            throw IOException("Unable to quarantine Autofill Inbox record")
        }
        if (!destination.setLastModified(nowMs())) {
            if (!destination.renameTo(source)) {
                throw IOException("Unable to restore Inbox record after quarantine timestamp failure")
            }
            throw IOException("Unable to set Autofill Inbox quarantine time")
        }
    }

    @Synchronized
    fun clear() {
        if (root.exists() && !root.deleteRecursively()) {
            throw IOException("Unable to clear Autofill Inbox storage")
        }
    }

    private fun cleanup(currentKeyName: String) {
        if (!root.exists()) return
        requireContained(root)
        root.listFiles().orEmpty().forEach { keyDirectory ->
            if (!keyDirectory.isDirectory || !KEY_ID_PATTERN.matches(keyDirectory.name)) {
                return@forEach
            }
            requireContained(keyDirectory)
            if (keyDirectory.name != currentKeyName) {
                if (!keyDirectory.deleteRecursively()) {
                    throw IOException("Unable to remove stale Autofill Inbox key directory")
                }
                return@forEach
            }
            val now = nowMs()
            val pendingDirectory = child(keyDirectory, PENDING_DIRECTORY)
            cleanupRecords(
                directory = pendingDirectory,
                now = now,
                retentionMs = AutofillInboxLimits.PENDING_RETENTION_MS,
            )

            val quarantineDirectory = child(keyDirectory, QUARANTINE_DIRECTORY)
            cleanupRecords(
                directory = quarantineDirectory,
                now = now,
                retentionMs = AutofillInboxLimits.QUARANTINE_RETENTION_MS,
            )
        }
    }

    private fun cleanupRecords(directory: File, now: Long, retentionMs: Long) {
        directory.listFiles().orEmpty().filter(File::isFile).forEach { file ->
            when {
                isTemporary(file) -> if (!file.delete()) throw IOException("Unable to remove Inbox temp file")
                !DRAFT_ID_PATTERN.matches(file.name) ->
                    if (!file.delete()) throw IOException("Unable to remove malformed Inbox file")
                isExpired(file.lastModified(), now, retentionMs) ->
                    if (!file.delete()) throw IOException("Unable to expire Autofill Inbox record")
            }
        }
    }

    private fun filesInAllKeys(directoryName: String): List<File> =
        root.listFiles().orEmpty()
            .filter { it.isDirectory && KEY_ID_PATTERN.matches(it.name) }
            .flatMap { keyDirectory ->
                child(keyDirectory, directoryName).listFiles().orEmpty()
                    .filter { it.isFile && !isTemporary(it) }
            }

    private fun allFiles(): List<File> = root.walkTopDown().filter(File::isFile).toList()

    private fun recordFile(
        keyId: ByteArray,
        directoryName: String,
        draftId: AutofillInboxDraftId,
    ): File = child(child(child(root, keyId.toHexKey()), directoryName), draftId.value)

    private fun listRecordFiles(directory: File): List<File> =
        if (!directory.isDirectory) emptyList()
        else directory.listFiles().orEmpty().filter { file ->
            requireContained(file)
            file.isFile && DRAFT_ID_PATTERN.matches(file.name)
        }

    private fun child(parent: File, name: String): File {
        val child = File(parent, name)
        requireContained(child)
        return child
    }

    private fun requireContained(file: File) {
        val canonicalRoot = root.canonicalFile.toPath()
        val canonicalFile = file.canonicalFile.toPath()
        require(canonicalFile == canonicalRoot || canonicalFile.startsWith(canonicalRoot)) {
            "Autofill Inbox path escaped its root"
        }
    }

    private fun ensureDirectory(directory: File) {
        requireContained(directory)
        if (!directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Unable to create Autofill Inbox directory")
        }
    }

    private fun isExpired(modifiedAtMs: Long, now: Long, retentionMs: Long): Boolean =
        modifiedAtMs <= now && now - modifiedAtMs >= retentionMs

    private fun isTemporary(file: File): Boolean =
        file.name.startsWith(TEMP_PREFIX) && file.name.endsWith(TEMP_SUFFIX)

    private fun ByteArray.toHexKey(): String {
        require(size == KEY_ID_BYTES) { "Inbox key ID must contain 16 bytes" }
        return joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private companion object {
        const val KEY_ID_BYTES = 16
        const val PENDING_DIRECTORY = "pending"
        const val QUARANTINE_DIRECTORY = "quarantine"
        const val TEMP_PREFIX = ".inbox-"
        const val TEMP_SUFFIX = ".tmp"
        val KEY_ID_PATTERN = Regex("[0-9a-f]{32}")
        val DRAFT_ID_PATTERN = Regex("[0-9a-f]{32}")
    }
}
