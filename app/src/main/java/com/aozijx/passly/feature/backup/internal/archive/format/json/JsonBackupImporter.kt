package com.aozijx.passly.feature.backup.internal.archive.format.json

import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.io.decodeStrictUtf8
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.JsonBackupPackage
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON 备份导入器。
 *
 * 将 JSON 字符串反序列化为 [BackupBundle]。
 */
@Singleton
class JsonBackupImporter @Inject constructor() {

    private fun decode(json: String): BackupBundle {
        val backupPackage = BackupJson.decodeFromString<JsonBackupPackage>(json)
        val resourceData = linkedMapOf<String, ByteArray>()
        try {
            backupPackage.resourcesBase64.forEach { (id, encoded) ->
                require(encoded.length <= MAX_ENCODED_RESOURCE_CHARS) {
                    "备份资源 Base64 过大: $id"
                }
                resourceData[id] = try {
                    Base64.getDecoder().decode(encoded)
                } catch (error: IllegalArgumentException) {
                    throw IllegalArgumentException("备份资源 Base64 无效: $id", error)
                }
            }
        } catch (error: Throwable) {
            resourceData.values.forEach { it.fill(0) }
            throw error
        }
        val bundle = BackupBundle(backupPackage.document, resourceData)
        return try {
            BackupBundleValidator.validate(
                bundle,
                requireResourceData = bundle.document.resources.isNotEmpty()
            )
            bundle
        } catch (error: Throwable) {
            resourceData.values.forEach { it.fill(0) }
            throw error
        }
    }

    fun import(bytes: ByteArray): BackupBundle {
        return decode(bytes.decodeStrictUtf8("备份 JSON"))
    }

    private companion object {
        const val MAX_ENCODED_RESOURCE_CHARS =
            (BackupBundleValidator.MAX_RESOURCE_BYTES * 4 / 3) + 8
    }
}
