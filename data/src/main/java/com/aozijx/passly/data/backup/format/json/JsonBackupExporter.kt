package com.aozijx.passly.data.backup.format.json

import com.aozijx.passly.data.backup.BackupBundleValidator
import com.aozijx.passly.data.backup.BackupJson
import com.aozijx.passly.data.backup.model.BackupBundle
import com.aozijx.passly.data.backup.model.JsonBackupPackage
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON 备份导出器。
 *
 * 将 [BackupBundle] 导出为完整 JSON 字符串。
 * 如果包含资源，资源内容以 Base64 嵌入。
 */
@Singleton
class JsonBackupExporter @Inject constructor() {

    fun export(bundle: BackupBundle): String {
        BackupBundleValidator.validate(
            bundle,
            requireResourceData = bundle.document.resources.isNotEmpty()
        )
        val encodedResources = bundle.resourceData
            .toSortedMap()
            .mapValues { (_, content) -> Base64.getEncoder().encodeToString(content) }
        return BackupJson.encodeToString(
            JsonBackupPackage(
                document = bundle.document,
                resourcesBase64 = encodedResources
            )
        )
    }
}
