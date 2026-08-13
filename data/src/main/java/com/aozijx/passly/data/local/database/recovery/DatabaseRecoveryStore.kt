package com.aozijx.passly.data.local.database.recovery

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.database.model.DatabaseRecoveryStatus
import com.aozijx.passly.data.local.database.DatabaseSchema
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 将当前保险库文件保存为私有恢复包，再清空活动位置。
 *
 * 恢复包不会进入 Android Auto Backup，且在完整复制成功前不会删除源文件。
 */
@Singleton
class DatabaseRecoveryStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val RECOVERY_FORMAT_VERSION = 2
        const val RECOVERY_ROOT = "database_recovery"
        const val RECOVERY_STATE_ROOT = "database_recovery_state"
        const val MANIFEST_FILE = "manifest.properties"
        const val MAX_MANIFEST_BYTES = 16L * 1024L
        const val MAX_PACKAGE_BYTES = 4L * 1024L * 1024L * 1024L
        const val MAX_REPORT_BYTES = 512 * 1024
        val RECOVERY_ID = Regex("[0-9]{1,17}-[0-9a-fA-F-]{36}")
        val RESOURCE_DIRECTORIES = VaultResourcePaths.RESOURCE_DIRECTORY_NAMES
        val EXPECTED_MANIFEST_KEYS = setOf(
            "formatVersion",
            "databaseName",
            "createdAtEpochMs",
            "databaseFiles",
            "resourceDirectories",
            "fileCount",
            "totalBytes",
            "contentSha256",
        )
    }

    class VerifiedPackage internal constructor(
        val info: DatabaseRecoveryPackage,
        val root: File,
        val databaseDirectory: File,
        val resourcesDirectory: File,
    )

    fun preserveAndClearActiveVault(): String? {
        val databaseFile = context.getDatabasePath(DatabaseSchema.DATABASE_NAME)
        val databaseFiles = databaseCandidates(databaseFile).filter(File::exists)
        val resourceDirectories = RESOURCE_DIRECTORIES
            .map { File(context.filesDir, it) }
            .filter(File::exists)
        if (databaseFiles.isEmpty() && resourceDirectories.isEmpty()) return null

        val recoveryId = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
        val recoveryRoot = recoveryRoot()
        val staging = File(recoveryRoot, ".$recoveryId.staging")
        val completed = File(recoveryRoot, recoveryId)

        check(recoveryRoot.exists() || recoveryRoot.mkdirs()) {
            "Unable to create database recovery directory"
        }
        check(staging.mkdir()) { "Unable to create database recovery staging directory" }

        try {
            val databaseTarget = File(staging, "database").also { target ->
                check(target.mkdir()) { "Unable to create recovery database directory" }
            }
            databaseFiles.forEach { source ->
                source.copyTo(File(databaseTarget, source.name), overwrite = false)
            }

            val resourcesTarget = File(staging, "resources")
            if (resourceDirectories.isNotEmpty()) {
                check(resourcesTarget.mkdir()) {
                    "Unable to create recovery resources directory"
                }
                resourceDirectories.forEach { source ->
                    check(source.copyRecursively(File(resourcesTarget, source.name))) {
                        "Unable to preserve Vault resource directory: ${source.name}"
                    }
                }
            }

            val contentFiles = staging.walkTopDown()
                .filter(File::isFile)
                .sortedBy { it.relativeTo(staging).invariantSeparatorsPath }
                .toList()
            val totalBytes = contentFiles.sumOf(File::length)
            check(totalBytes <= MAX_PACKAGE_BYTES) { "Database recovery package is too large" }
            val contentDigest = contentDigest(staging, contentFiles)

            File(staging, MANIFEST_FILE).writeText(
                buildString {
                    appendLine("formatVersion=$RECOVERY_FORMAT_VERSION")
                    appendLine("databaseName=${DatabaseSchema.DATABASE_NAME}")
                    appendLine("createdAtEpochMs=${System.currentTimeMillis()}")
                    appendLine("databaseFiles=${databaseFiles.joinToString(",") { it.name }}")
                    appendLine(
                        "resourceDirectories=" +
                            resourceDirectories.joinToString(",") { it.name }
                    )
                    appendLine("fileCount=${contentFiles.size}")
                    appendLine("totalBytes=$totalBytes")
                    appendLine("contentSha256=$contentDigest")
                }
            )

            check(staging.renameTo(completed)) {
                "Unable to finalize database recovery package"
            }
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }

        clearActiveDatabase(databaseFile)
        resourceDirectories.forEach { source ->
            if (source.exists() && !source.deleteRecursively()) {
                throw IOException("Unable to clear Vault resource directory: ${source.name}")
            }
        }
        return recoveryId
    }

    fun listPackages(): List<DatabaseRecoveryPackage> {
        val root = recoveryRoot()
        if (!root.isDirectory) return emptyList()
        return root.listFiles().orEmpty()
            .asSequence()
            .filter { it.isDirectory && !it.name.startsWith('.') }
            .mapNotNull { directory -> runCatching { verify(directory.name).info }.getOrNull() }
            .sortedByDescending(DatabaseRecoveryPackage::createdAtEpochMs)
            .toList()
    }

    fun verify(packageId: String): VerifiedPackage {
        require(RECOVERY_ID.matches(packageId)) { "Invalid database recovery package ID" }
        val root = recoveryRoot().canonicalFile
        val packageRoot = File(root, packageId).canonicalFile
        require(packageRoot.parentFile == root && packageRoot.isDirectory) {
            "Database recovery package does not exist"
        }
        val manifestFile = File(packageRoot, MANIFEST_FILE)
        require(manifestFile.isFile && manifestFile.length() in 1..MAX_MANIFEST_BYTES) {
            "Invalid database recovery manifest"
        }
        val manifest = parseManifest(manifestFile)
        require(manifest.keys == EXPECTED_MANIFEST_KEYS) { "Unexpected recovery manifest fields" }
        require(manifest.getValue("formatVersion").toInt() == RECOVERY_FORMAT_VERSION) {
            "Unsupported database recovery format"
        }
        require(manifest.getValue("databaseName") == DatabaseSchema.DATABASE_NAME) {
            "Unexpected recovery database name"
        }
        val createdAt = manifest.getValue("createdAtEpochMs").toLong()
        require(createdAt > 0L && createdAt <= System.currentTimeMillis() + 60_000L) {
            "Invalid recovery creation time"
        }
        val expectedDatabaseFiles = manifest.getValue("databaseFiles")
            .split(',').filter(String::isNotBlank)
        require(expectedDatabaseFiles.isNotEmpty()) { "Recovery database is missing" }
        require(expectedDatabaseFiles.all { it in allowedDatabaseFileNames() }) {
            "Unexpected database file in recovery manifest"
        }
        val expectedResources = manifest.getValue("resourceDirectories")
            .split(',').filter(String::isNotBlank)
        require(expectedResources.all { it in RESOURCE_DIRECTORIES }) {
            "Unexpected resource directory in recovery manifest"
        }

        val databaseDirectory = File(packageRoot, "database").canonicalFile
        val resourcesDirectory = File(packageRoot, "resources").canonicalFile
        require(databaseDirectory.parentFile == packageRoot && databaseDirectory.isDirectory) {
            "Recovery database directory is missing"
        }
        val actualDatabaseFiles = databaseDirectory.listFiles().orEmpty()
            .filter(File::isFile).map(File::getName).sorted()
        require(actualDatabaseFiles == expectedDatabaseFiles.sorted()) {
            "Recovery database files do not match the manifest"
        }
        if (expectedResources.isEmpty()) {
            require(!resourcesDirectory.exists()) { "Unexpected recovery resources" }
        } else {
            require(resourcesDirectory.parentFile == packageRoot && resourcesDirectory.isDirectory) {
                "Recovery resources directory is missing"
            }
            val actualResources = resourcesDirectory.listFiles().orEmpty()
                .filter(File::isDirectory).map(File::getName).sorted()
            require(actualResources == expectedResources.sorted()) {
                "Recovery resource directories do not match the manifest"
            }
        }

        val contentFiles = packageRoot.walkTopDown()
            .filter(File::isFile)
            .filterNot { it.name == MANIFEST_FILE }
            .onEach { file ->
                require(file.canonicalPath.startsWith(packageRoot.path + File.separator)) {
                    "Recovery package path escaped its root"
                }
            }
            .sortedBy { it.relativeTo(packageRoot).invariantSeparatorsPath }
            .toList()
        val totalBytes = contentFiles.sumOf(File::length)
        require(totalBytes <= MAX_PACKAGE_BYTES) { "Database recovery package is too large" }
        require(contentFiles.size == manifest.getValue("fileCount").toInt()) {
            "Recovery file count does not match"
        }
        require(totalBytes == manifest.getValue("totalBytes").toLong()) {
            "Recovery size does not match"
        }
        require(contentDigest(packageRoot, contentFiles) == manifest.getValue("contentSha256")) {
            "Recovery content digest does not match"
        }
        return VerifiedPackage(
            info = DatabaseRecoveryPackage(
                id = packageId,
                createdAtEpochMs = createdAt,
                sizeBytes = totalBytes,
                status = readStatus(packageId),
            ),
            root = packageRoot,
            databaseDirectory = databaseDirectory,
            resourcesDirectory = resourcesDirectory,
        )
    }

    fun updateStatus(packageId: String, status: DatabaseRecoveryStatus) {
        verify(packageId)
        val stateRoot = stateRoot().also { check(it.exists() || it.mkdirs()) }
        val target = File(stateRoot, "$packageId.status").canonicalFile
        require(target.parentFile == stateRoot.canonicalFile) { "Recovery state path escaped its root" }
        val staging = File(stateRoot, ".$packageId.status.tmp")
        staging.writeText(status.name)
        check(staging.renameTo(target) || run {
            target.delete() && staging.renameTo(target)
        }) { "Unable to update database recovery status" }
    }

    fun writeEncryptedReport(packageId: String, encryptedReport: ByteArray) {
        verify(packageId)
        require(encryptedReport.isNotEmpty() && encryptedReport.size <= MAX_REPORT_BYTES) {
            "Invalid database recovery report"
        }
        val stateRoot = stateRoot().also { check(it.exists() || it.mkdirs()) }
        val target = File(stateRoot, "$packageId.report.enc").canonicalFile
        require(target.parentFile == stateRoot.canonicalFile) { "Recovery report path escaped its root" }
        val staging = File(stateRoot, ".$packageId.report.tmp")
        staging.writeBytes(encryptedReport)
        check(staging.renameTo(target) || run {
            target.delete() && staging.renameTo(target)
        }) { "Unable to persist database recovery report" }
    }

    fun delete(packageId: String) {
        val verified = verify(packageId)
        check(verified.root.deleteRecursively()) { "Unable to delete database recovery package" }
        File(stateRoot(), "$packageId.status").delete()
        File(stateRoot(), "$packageId.report.enc").delete()
    }

    private fun clearActiveDatabase(databaseFile: File) {
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        val remaining = databaseCandidates(databaseFile).filter(File::exists)
        if (remaining.isNotEmpty()) {
            throw IOException(
                "Unable to clear active database files: ${remaining.joinToString { it.name }}"
            )
        }
    }

    private fun databaseCandidates(databaseFile: File): List<File> = listOf(
        databaseFile,
        File(databaseFile.path + "-wal"),
        File(databaseFile.path + "-shm"),
        File(databaseFile.path + "-journal")
    )

    private fun recoveryRoot(): File = File(context.noBackupFilesDir, RECOVERY_ROOT)

    private fun stateRoot(): File = File(context.noBackupFilesDir, RECOVERY_STATE_ROOT)

    private fun readStatus(packageId: String): DatabaseRecoveryStatus = runCatching {
        DatabaseRecoveryStatus.valueOf(File(stateRoot(), "$packageId.status").readText().trim())
    }.getOrDefault(DatabaseRecoveryStatus.PENDING_SCAN)

    private fun allowedDatabaseFileNames(): Set<String> {
        val name = DatabaseSchema.DATABASE_NAME
        return setOf(name, "$name-wal", "$name-shm", "$name-journal")
    }

    private fun parseManifest(file: File): Map<String, String> {
        val result = linkedMapOf<String, String>()
        file.readLines().forEach { line ->
            require(line.isNotBlank() && !line.startsWith('#')) { "Invalid recovery manifest line" }
            val separator = line.indexOf('=')
            require(separator > 0) { "Invalid recovery manifest entry" }
            val key = line.substring(0, separator)
            val value = line.substring(separator + 1)
            require(result.put(key, value) == null) { "Duplicate recovery manifest field" }
        }
        return result
    }

    private fun contentDigest(root: File, files: List<File>): String {
        val aggregate = MessageDigest.getInstance("SHA-256")
        files.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            aggregate.update(relative.toByteArray(Charsets.UTF_8))
            aggregate.update(0.toByte())
            aggregate.update(file.length().toString().toByteArray(Charsets.US_ASCII))
            aggregate.update(0.toByte())
            val fileDigest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    fileDigest.update(buffer, 0, count)
                }
            }
            aggregate.update(fileDigest.digest())
            aggregate.update('\n'.code.toByte())
        }
        return aggregate.digest().joinToString("") { "%02x".format(it) }
    }

}
