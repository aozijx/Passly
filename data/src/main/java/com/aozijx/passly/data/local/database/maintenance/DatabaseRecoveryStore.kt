package com.aozijx.passly.data.local.database.maintenance

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.local.database.DatabaseSchema
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
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
        const val RECOVERY_FORMAT_VERSION = 1
        const val RECOVERY_ROOT = "database_recovery"
        val RESOURCE_DIRECTORIES = VaultResourcePaths.RESOURCE_DIRECTORY_NAMES
    }

    fun preserveAndClearActiveVault(): String? {
        val databaseFile = context.getDatabasePath(DatabaseSchema.DATABASE_NAME)
        val databaseFiles = databaseCandidates(databaseFile).filter(File::exists)
        val resourceDirectories = RESOURCE_DIRECTORIES
            .map { File(context.filesDir, it) }
            .filter(File::exists)
        if (databaseFiles.isEmpty() && resourceDirectories.isEmpty()) return null

        val recoveryId = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
        val recoveryRoot = File(context.noBackupFilesDir, RECOVERY_ROOT)
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

            File(staging, "manifest.properties").writeText(
                buildString {
                    appendLine("formatVersion=$RECOVERY_FORMAT_VERSION")
                    appendLine("databaseName=${DatabaseSchema.DATABASE_NAME}")
                    appendLine("createdAtEpochMs=${System.currentTimeMillis()}")
                    appendLine("databaseFiles=${databaseFiles.joinToString(",") { it.name }}")
                    appendLine(
                        "resourceDirectories=" +
                            resourceDirectories.joinToString(",") { it.name }
                    )
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
}
