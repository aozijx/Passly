package com.aozijx.passly.app.database.backup

import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord
import com.aozijx.passly.feature.backup.internal.archive.snapshot.RestoreFileJournal
import java.io.File

internal object RoomBackupFaviconResource {
    private const val MIME_TYPE_WEBP = "image/webp"

    data class Exported(
        val record: BackupResourceRecord,
        val content: ByteArray,
    )

    fun export(entryId: String, iconPath: String, iconRoot: File): Exported? {
        val root = iconRoot.canonicalFile
        val iconFile = File(iconPath).canonicalFile
        require(iconFile != root && iconFile.toPath().startsWith(root.toPath())) {
            "图标路径超出应用图标目录: $entryId"
        }
        if (!iconFile.isFile) return null
        require(iconFile.extension.equals("webp", ignoreCase = true)) {
            "图标不是 WebP 格式: $entryId"
        }
        require(iconFile.length() <= BackupBundleValidator.MAX_RESOURCE_BYTES) {
            "图标文件过大: $entryId"
        }

        val content = iconFile.readBytes()
        return Exported(
            record = BackupResourceRecord(
                id = "icon_$entryId",
                entryId = entryId,
                kind = BackupResourceKind.ICON,
                fileName = iconFile.name,
                mimeType = MIME_TYPE_WEBP,
                size = content.size.toLong(),
                sha256 = BackupBundleValidator.sha256Hex(content),
            ),
            content = content,
        )
    }

    fun restore(
        record: BackupResourceRecord,
        content: ByteArray,
        iconRoot: File,
        fileJournal: RestoreFileJournal,
    ): File {
        require(record.kind == BackupResourceKind.ICON) { "资源不是图标: ${record.id}" }
        require(record.mimeType == MIME_TYPE_WEBP) { "图标不是 WebP 格式: ${record.id}" }
        val root = iconRoot.canonicalFile.apply { mkdirs() }
        val target = File(
            root,
            "restored_${
                BackupBundleValidator.sha256Hex(record.id.toByteArray()).take(32)
            }.webp",
        )
        fileJournal.replace(target, content)
        return target.canonicalFile
    }
}
