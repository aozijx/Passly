package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.BackupImportPlanner
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.io.decodeStrictUtf8
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import javax.inject.Inject
import javax.inject.Singleton
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy

/**
 * 加密备份导入器。
 *
 * 反向流程：
 * 1. 解密加密容器得到 ZIP
 * 2. 从 ZIP 中提取 JSON 文档和资源
 * 3. 反序列化为 BackupBundle
 */
@Singleton
class EncryptedBackupImporter @Inject constructor() {

    fun import(
        container: ByteArray,
        password: CharArray,
        strategy: BackupImportStrategy = BackupImportStrategy.STRICT,
    ): BackupBundle {
        try {
            val zipContent = EncryptedBackupContainerCodec.decrypt(container, password)
            return try {
                val archiveContent = BackupArchiveCodec.readZip(zipContent)
                var handedOff = false
                try {
                    val document = BackupJson.decodeFromString<BackupDocument>(
                        archiveContent.documentJson.decodeStrictUtf8("document.json")
                    )
                    val resourceData = archiveContent.resources.mapKeys { (name, _) ->
                        name.removePrefix(BackupArchiveCodec.RESOURCE_ENTRY_PREFIX)
                    }
                    val bundle = BackupBundle(document = document, resourceData = resourceData)
                    val planned = BackupImportPlanner.plan(bundle, strategy).bundle
                    handedOff = true
                    planned
                } finally {
                    archiveContent.documentJson.fill(0)
                    if (!handedOff) {
                        archiveContent.resources.values.forEach { it.fill(0) }
                    }
                }
            } finally {
                zipContent.fill(0)
            }
        } catch (error: BackupFailed) {
            throw error
        } catch (error: Exception) {
            throw BackupFailed(
                throwableType = error.javaClass.simpleName
            )
        }
    }
}
