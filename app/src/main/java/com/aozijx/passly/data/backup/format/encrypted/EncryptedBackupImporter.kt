package com.aozijx.passly.data.backup.format.encrypted

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.data.backup.BackupBundleValidator
import com.aozijx.passly.data.backup.BackupJson
import com.aozijx.passly.data.backup.io.decodeStrictUtf8
import com.aozijx.passly.data.backup.model.BackupBundle
import com.aozijx.passly.data.backup.model.BackupDocument
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

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

    fun import(container: ByteArray, password: CharArray): BackupBundle {
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
                    BackupBundleValidator.validate(
                        bundle,
                        requireResourceData = document.resources.isNotEmpty()
                    )
                    handedOff = true
                    bundle
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
            val authenticationFailed = generateSequence<Throwable>(error) { it.cause }
                .any { it is AEADBadTagException }
            val message = if (authenticationFailed) {
                "备份密码错误，或备份文件已损坏"
            } else {
                "备份文件损坏或格式不正确"
            }
            throw BackupFailed()
        }
    }
}
