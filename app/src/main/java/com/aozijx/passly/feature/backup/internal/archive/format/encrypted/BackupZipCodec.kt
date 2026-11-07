package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.io.WipingByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Reads and writes the ZIP entries inside a Passly encrypted container. */
internal object BackupZipCodec {
    const val DOCUMENT_ENTRY_NAME = "document.json"
    const val RESOURCE_ENTRY_PREFIX = "resources/"

    fun buildZip(
        documentJson: ByteArray,
        resources: Map<String, ByteArray>,
        output: OutputStream,
    ) {
        require(documentJson.size <= BackupBundleValidator.MAX_DOCUMENT_BYTES) { "备份文档过大" }
        require(resources.size <= BackupBundleValidator.MAX_RESOURCES) { "备份资源数量过多" }
        val totalBytes =
            resources.values.fold(documentJson.size.toLong()) { total, data ->
                Math.addExact(total, data.size.toLong())
            }
        require(totalBytes <= BackupBundleValidator.MAX_ARCHIVE_BYTES) { "备份归档解压后过大" }

        ZipOutputStream(NonClosingOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry(DOCUMENT_ENTRY_NAME))
            zip.write(documentJson)
            zip.closeEntry()
            resources.toSortedMap().forEach { (name, data) ->
                validateEntryName(name)
                require(data.size <= BackupBundleValidator.MAX_RESOURCE_BYTES) { "资源文件过大: $name" }
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
    }

    fun readZip(input: InputStream): ZipContent {
        var documentJson: ByteArray? = null
        val resources = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        var entryCount = 0
        try {
            ZipInputStream(NonClosingInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    entryCount++
                    require(entryCount <= BackupBundleValidator.MAX_ENTRIES + 1) { "备份归档条目数量过多" }
                    require(!entry.isDirectory) { "备份中不允许目录条目" }
                    when (entry.name) {
                        DOCUMENT_ENTRY_NAME -> {
                            require(documentJson == null) { "备份包含重复文档条目" }
                            val content = zip.readLimited(BackupBundleValidator.MAX_DOCUMENT_BYTES)
                            documentJson = content
                            totalBytes = Math.addExact(totalBytes, content.size.toLong())
                        }

                        else -> {
                            validateEntryName(entry.name)
                            val content = zip.readLimited(BackupBundleValidator.MAX_RESOURCE_BYTES)
                            require(resources.put(entry.name, content) == null) {
                                "备份包含重复资源条目: ${entry.name}"
                            }
                            totalBytes = Math.addExact(totalBytes, content.size.toLong())
                        }
                    }
                    require(totalBytes <= BackupBundleValidator.MAX_ARCHIVE_BYTES) { "备份归档解压后过大" }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            return ZipContent(
                documentJson = requireNotNull(documentJson) { "备份缺少文档条目" },
                resources = resources,
            )
        } catch (error: Throwable) {
            documentJson?.fill(0)
            resources.values.forEach { it.fill(0) }
            throw error
        }
    }

    data class ZipContent(
        val documentJson: ByteArray,
        val resources: Map<String, ByteArray>,
    )

    private fun validateEntryName(name: String) {
        require(name.startsWith(RESOURCE_ENTRY_PREFIX)) { "未知备份条目: $name" }
        require(name.length > RESOURCE_ENTRY_PREFIX.length) { "备份资源名称为空" }
        require(".." !in name && '\\' !in name && !name.startsWith('/')) { "非法备份路径: $name" }
    }

    private fun ZipInputStream.readLimited(maxBytes: Int): ByteArray {
        val output = WipingByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE, maxBytes))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        try {
            while (true) {
                val count = read(buffer)
                if (count < 0) break
                require(count <= maxBytes - total) { "备份条目超过大小限制" }
                total += count
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        } finally {
            buffer.fill(0)
            output.wipe()
        }
    }

    private class NonClosingInputStream(input: InputStream) : FilterInputStream(input) {
        override fun close() = Unit
    }

    private class NonClosingOutputStream(output: OutputStream) : OutputStream() {
        private val delegate = output

        override fun write(value: Int) = delegate.write(value)

        override fun write(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ) = delegate.write(buffer, offset, length)

        override fun flush() = delegate.flush()

        override fun close() = Unit
    }
}
