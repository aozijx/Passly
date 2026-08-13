package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 加密备份导出器。
 *
 * 组合流程：
 * 1. 将 BackupBundle 序列化为 JSON
 * 2. 将 JSON + 资源打包为 ZIP
 * 3. 将 ZIP 加密为加密容器
 */
@Singleton
class EncryptedBackupExporter @Inject constructor() {

    fun export(bundle: BackupBundle, password: CharArray): ByteArray {
        BackupBundleValidator.validate(
            bundle,
            requireResourceData = bundle.document.resources.isNotEmpty()
        )
        val documentJson = BackupJson.encodeToString(bundle.document).toByteArray(Charsets.UTF_8)
        val resourceEntries = bundle.resourceData.mapKeys { (id, _) -> "resources/$id" }
        val zipContent = BackupArchiveCodec.buildZip(documentJson, resourceEntries)
        return try {
            EncryptedBackupContainerCodec.encrypt(zipContent, password)
        } finally {
            documentJson.fill(0)
            zipContent.fill(0)
        }
    }
}
