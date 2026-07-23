package com.aozijx.passly.data.repository.backup

import android.content.Context
import androidx.core.net.toUri
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.local.database.maintenance.VaultDatabaseCleaner
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.data.mapper.assembler.VaultEntryAssembler
import com.aozijx.passly.data.mapper.snapshot.toSnapshot
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.data.repository.backup.internal.BackupArchiveCodec
import com.aozijx.passly.data.repository.backup.internal.BackupArchiveContent
import com.aozijx.passly.di.IoDispatcher
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.model.backup.ImportMode
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.backup.BackupRepository
import com.aozijx.passly.security.crypto.CryptoEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BackupRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cryptoEngine: CryptoEngine,
    private val sessionManager: UnifiedSessionManager,
    private val stateProvider: SessionStateProvider,
    private val cryptoMapper: VaultEntryCryptoMapper,
    private val vaultDatabaseCleaner: VaultDatabaseCleaner,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BackupRepository {

    private suspend fun getVaultEntries(): List<VaultEntry> {
        return sessionManager.query {
            val metadataEntities = metadataDao().getActive()
            val credentialEntities =
                credentialDao().getByEntryIds(metadataEntities.map { it.entryId })
            val credentialMap = credentialEntities.associateBy { it.entryId }

            metadataEntities.map { metaEntity ->
                val meta = cryptoMapper.decryptMetadata(metaEntity)
                val cred = credentialMap[metaEntity.entryId]
                    ?.let { cryptoMapper.decryptCredential(it) }
                VaultEntryAssembler.assembleFromDatabase(metaEntity, meta, cred)
            }
        }
    }

    private fun imageEntryName(entryId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(entryId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "images/${digest.take(32)}.bin"
    }

    override suspend fun exportEncryptedBackup(
        uri: String,
        password: CharArray,
        includeImages: Boolean
    ): AppResult<Unit> {
        return withContext(ioDispatcher) {
            AppResult.runSuspendCatching("backup.export.encrypted") {
                val entries = getVaultEntries()
                val images = linkedMapOf<String, ByteArray>()
                val snapshots = entries.map { entry ->
                    val snapshot = entry.toSnapshot()
                    val source = entry.iconCustomPath?.let(::File)
                    if (includeImages && source?.isFile == true) {
                        val archivePath = imageEntryName(entry.id)
                        images[archivePath] = source.readBytes()
                        snapshot.copy(
                            metadata = snapshot.metadata.copy(iconCustomPath = archivePath)
                        )
                    } else {
                        snapshot.copy(metadata = snapshot.metadata.copy(iconCustomPath = null))
                    }
                }
                val snapshotJson = AppJson.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(VaultSnapshot.serializer()),
                    snapshots
                ).toByteArray(Charsets.UTF_8)
                val encoded = BackupArchiveCodec.encode(
                    BackupArchiveContent(snapshotJson, images),
                    password
                )
                val output = context.contentResolver.openOutputStream(uri.toUri())
                    ?: error("无法打开备份输出文件")
                output.use {
                    it.write(encoded)
                    it.flush()
                }
            }
        }
    }

    override suspend fun exportPlainBackup(uri: String): AppResult<Unit> {
        return withContext(ioDispatcher) {
            AppResult.runSuspendCatching("backup.export.plain") {
                val entries = getVaultEntries()
                val snapshots = entries.map { it.toSnapshot() }

                val backupData = AppJson.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(VaultSnapshot.serializer()),
                    snapshots
                )

                val output = context.contentResolver.openOutputStream(uri.toUri())
                    ?: error("无法打开明文备份输出文件")
                output.use {
                    it.write(backupData.toByteArray(Charsets.UTF_8))
                    it.flush()
                }
            }
        }
    }

    override suspend fun exportEmergencyBackup(): AppResult<File> {
        return withContext(ioDispatcher) {
            EmergencyBackupExporter.exportOnFailure(context, cryptoEngine)
        }
    }

    override suspend fun importBackup(
        uri: String,
        password: CharArray,
        config: ImportMode
    ): AppResult<Unit> {
        return withContext(ioDispatcher) {
            AppResult.runSuspendCatching("backup.import") {
                val input = context.contentResolver.openInputStream(uri.toUri())
                    ?: error("无法打开备份文件")
                val encoded = input.use { it.readBytes() }
                val archive = BackupArchiveCodec.decode(encoded, password)
                val snapshots = decodeSnapshots(archive.snapshotJson)
                importWithImages(snapshots, archive.images, config)
            }
        }
    }

    private fun decodeSnapshots(bytes: ByteArray): List<VaultSnapshot> =
        AppJson.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(VaultSnapshot.serializer()),
            String(bytes, Charsets.UTF_8)
        )

    private suspend fun importWithImages(
        snapshots: List<VaultSnapshot>,
        images: Map<String, ByteArray>,
        config: ImportMode
    ) {
        val createdFiles = mutableListOf<File>()
        val restored = snapshots.map { snapshot ->
            val archivePath = snapshot.metadata.iconCustomPath
            val imageBytes = archivePath?.let(images::get)
            if (archivePath == null) {
                snapshot.copy(metadata = snapshot.metadata.copy(iconCustomPath = null))
            } else {
                requireNotNull(imageBytes) { "备份缺少附件: $archivePath" }
                val directory = File(context.filesDir, "vault_images").apply { mkdirs() }
                val target = File(directory, "restored_${archivePath.substringAfterLast('/')}")
                target.outputStream().use { it.write(imageBytes) }
                createdFiles += target
                snapshot.copy(
                    metadata = snapshot.metadata.copy(iconCustomPath = target.absolutePath)
                )
            }
        }
        try {
            importSnapshots(restored, config)
        } catch (error: Throwable) {
            createdFiles.forEach(File::delete)
            throw error
        }
    }

    private suspend fun importSnapshots(snapshots: List<VaultSnapshot>, config: ImportMode) {
        if (config == ImportMode.OVERWRITE) {
            vaultDatabaseCleaner.clearVaultData()
        }
        sessionManager.transaction {

            snapshots.forEach { snapshot ->
                val entryId = snapshot.id
                val metaBlob = cryptoMapper.encryptMetadata(snapshot.metadata, entryId)
                val credBlob = cryptoMapper.encryptCredential(snapshot.credential, entryId)

                val metaEntity = VaultMetadataEntity(
                    entryId = entryId,
                    vaultId = snapshot.vaultId,
                    entryVersion = snapshot.revision,
                    entryType = snapshot.entryType,
                    metadataBlob = metaBlob,
                    createdAt = snapshot.createdAt,
                    updatedAt = snapshot.updatedAt,
                    deletedAt = snapshot.deletedAt
                )

                val credEntity = VaultCredentialEntity(
                    entryId = entryId,
                    credentialBlob = credBlob
                )

                metadataDao().upsertForImport(metaEntity)
                credentialDao().upsertForImport(credEntity)
            }
        }
    }

    override suspend fun importPlainBackup(
        uri: String,
        config: ImportMode
    ): AppResult<Unit> {
        return withContext(ioDispatcher) {
            AppResult.runSuspendCatching("backup.import.plain") {
                val input = context.contentResolver.openInputStream(uri.toUri())
                    ?: error("无法打开明文备份文件")
                input.use { inputStream ->
                    val backupData = inputStream.readBytes()
                    val snapshots = AppJson.decodeFromString(
                        kotlinx.serialization.builtins.ListSerializer(VaultSnapshot.serializer()),
                        String(backupData, Charsets.UTF_8)
                    ).map {
                        it.copy(metadata = it.metadata.copy(iconCustomPath = null))
                    }

                    importSnapshots(snapshots, config)
                }
            }
        }
    }

    override suspend fun checkDirectoryWritable(uri: String): AppResult<Unit> =
        withContext(ioDispatcher) {
            AppResult.runSuspendCatching("backup.checkWritable") {
                val parsedUri = uri.toUri()
                // 尝试创建临时文件测试写入权限
                context.contentResolver.openOutputStream(parsedUri)?.use {
                    it.write(ByteArray(0))
                    it.flush()
                } ?: error("无法打开目录，可能没有写入权限")
            }
        }
}
