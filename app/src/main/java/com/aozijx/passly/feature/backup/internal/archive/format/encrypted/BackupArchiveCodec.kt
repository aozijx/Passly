package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份归档编解码器。
 *
 * 只负责 ZIP 条目的创建与读取。
 * 加密/解密由 [EncryptedBackupContainerCodec] 处理。
 */
object BackupArchiveCodec {
    private const val MAX_ARCHIVE_BYTES = 128 * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 16 * 1024 * 1024
    private const val MAX_ENTRY_COUNT = 100_001
    private const val MAX_DOCUMENT_BYTES = 16 * 1024 * 1024
    const val DOCUMENT_ENTRY_NAME = "document.json"
    const val RESOURCE_ENTRY_PREFIX = "resources/"

    fun buildZip(documentJson: ByteArray, resources: Map<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { zip ->
                require(documentJson.size <= MAX_DOCUMENT_BYTES) { "备份文档过大" }
                require(resources.size < MAX_ENTRY_COUNT) { "备份资源数量过多" }
                val totalBytes = documentJson.size.toLong() +
                        resources.values.sumOf { it.size.toLong() }
                require(totalBytes <= MAX_ARCHIVE_BYTES) { "备份归档解压后过大" }
                zip.putNextEntry(ZipEntry(DOCUMENT_ENTRY_NAME))
                zip.write(documentJson)
                zip.closeEntry()
                resources.toSortedMap().forEach { (name, data) ->
                    validateEntryName(name)
                    require(data.size <= MAX_ENTRY_BYTES) { "资源文件过大: $name" }
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
            bytes.toByteArray()
        }

    fun readZip(bytes: ByteArray): ZipContent {
        var documentJson: ByteArray? = null
        val resources = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        var entryCount = 0
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    entryCount++
                    require(entryCount <= MAX_ENTRY_COUNT) { "备份归档条目数量过多" }
                    require(!entry.isDirectory) { "备份中不允许目录条目" }
                    when (entry.name) {
                        DOCUMENT_ENTRY_NAME -> {
                            require(documentJson == null) { "备份包含重复文档条目" }
                            val content = zip.readLimited(MAX_DOCUMENT_BYTES)
                            documentJson = content
                            totalBytes += content.size
                        }

                        else -> {
                            validateEntryName(entry.name)
                            val content = zip.readLimited(MAX_ENTRY_BYTES)
                            require(resources.put(entry.name, content) == null) {
                                "备份包含重复资源条目: ${entry.name}"
                            }
                            totalBytes += content.size
                        }
                    }
                    require(totalBytes <= MAX_ARCHIVE_BYTES) { "备份归档解压后过大" }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            return ZipContent(
                documentJson = requireNotNull(documentJson) { "备份缺少文档条目" },
                resources = resources
            )
        } catch (error: Throwable) {
            documentJson?.fill(0)
            resources.values.forEach { it.fill(0) }
            throw error
        }
    }

    data class ZipContent(
        val documentJson: ByteArray,
        val resources: Map<String, ByteArray>
    )

    private fun validateEntryName(name: String) {
        require(name.startsWith(RESOURCE_ENTRY_PREFIX)) { "未知备份条目: $name" }
        require(name.length > RESOURCE_ENTRY_PREFIX.length) { "备份资源名称为空" }
        require(".." !in name && '\\' !in name && !name.startsWith('/')) {
            "非法备份路径: $name"
        }
    }

    private fun ZipInputStream.readLimited(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            require(total <= maxBytes) { "备份条目超过大小限制" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
