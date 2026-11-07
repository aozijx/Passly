package com.aozijx.passly.feature.backup.internal.archive.format.json

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.format.BackupImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenExport
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenFolder
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenItem
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenJsonImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.decodeBitwardenRecord
import com.aozijx.passly.feature.backup.internal.archive.io.LimitedOutputStream
import com.aozijx.passly.feature.backup.internal.archive.io.WipingByteArrayOutputStream
import com.aozijx.passly.feature.backup.internal.archive.io.copyCurrentValueTo
import com.aozijx.passly.feature.backup.internal.archive.io.forEachObjectField
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import java.io.InputStream
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Detects JSON formats from root keys while consuming the source only once. */
@Singleton
internal class JsonFormatDispatcher
    @Inject
    constructor(
        private val passlyImporter: JsonBackupImporter,
        private val bitwardenImporter: BitwardenJsonImportAdapter,
    ) : BackupImportAdapter {
        override val formatId: BackupFormatId = BackupFormats.PASSLY_JSON
        override val requiresPassword: Boolean = false

        override fun decode(
            input: InputStream,
            password: CharArray?,
            strategy: BackupImportStrategy,
        ): BackupBundle {
            val document = WipingByteArrayOutputStream()
            val resources = LinkedHashMap<String, ByteArray>()
            val folders = mutableListOf<BitwardenFolder>()
            val items = mutableListOf<BitwardenItem>()
            var documentSeen = false
            var itemsSeen = false
            var resourcesSeen = false
            var foldersSeen = false
            var encrypted = false
            var encryptedSeen = false
            var transferred = false
            try {
                BackupJsonParserFactory.create(input).use { parser ->
                    require(parser.nextToken() == JsonToken.START_OBJECT) { "备份 JSON 根节点无效" }
                    parser.forEachObjectField { name ->
                        when (name) {
                            "document" -> {
                                require(!documentSeen && !itemsSeen) { "JSON 根对象格式不明确" }
                                documentSeen = true
                                require(parser.currentToken == JsonToken.START_OBJECT) { "备份 document 无效" }
                                val bounded = LimitedOutputStream(document, BackupBundleValidator.MAX_DOCUMENT_BYTES.toLong())
                                JsonFactory().createGenerator(bounded).use { generator ->
                                    generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                                    parser.copyCurrentValueTo(generator)
                                }
                            }

                            "items" -> {
                                require(!itemsSeen && !documentSeen) { "JSON 根对象格式不明确" }
                                itemsSeen = true
                                require(parser.currentToken == JsonToken.START_ARRAY) { "Bitwarden items 无效" }
                                while (parser.nextToken() != JsonToken.END_ARRAY) {
                                    require(items.size < BackupBundleValidator.MAX_ENTRIES) { "Bitwarden item 数量过多" }
                                    require(parser.currentToken == JsonToken.START_OBJECT) { "Bitwarden item 无效" }
                                    items += parser.decodeBitwardenRecord<BitwardenItem>()
                                }
                            }

                            "resourcesBase64" -> {
                                require(!resourcesSeen) { "备份包含重复 resourcesBase64" }
                                resourcesSeen = true
                                require(parser.currentToken == JsonToken.START_OBJECT) { "备份资源对象无效" }
                                resources.putAll(parser.readBase64Resources())
                            }

                            "folders" -> {
                                require(!foldersSeen) { "Bitwarden JSON 包含重复 folders 字段" }
                                foldersSeen = true
                                require(parser.currentToken == JsonToken.START_ARRAY) { "Bitwarden folders 无效" }
                                while (parser.nextToken() != JsonToken.END_ARRAY) {
                                    require(folders.size < BackupBundleValidator.MAX_ENTRIES) { "Bitwarden folder 数量过多" }
                                    require(parser.currentToken == JsonToken.START_OBJECT) { "Bitwarden folder 无效" }
                                    folders += parser.decodeBitwardenRecord<BitwardenFolder>()
                                }
                            }

                            "encrypted" -> {
                                require(!encryptedSeen) { "Bitwarden JSON 包含重复 encrypted 字段" }
                                encryptedSeen = true
                                val token = parser.currentToken
                                require(token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
                                    "Bitwarden encrypted 字段无效"
                                }
                                encrypted = parser.booleanValue
                            }

                            else -> parser.skipChildren()
                        }
                    }
                    require(parser.currentToken == JsonToken.END_OBJECT && parser.nextToken() == null) {
                        "备份 JSON 根对象无效"
                    }
                }

                val result =
                    when {
                        documentSeen && !itemsSeen -> {
                            val bytes = document.toByteArray()
                            try {
                                passlyImporter.importParsed(bytes, resources, strategy)
                            } finally {
                                bytes.fill(0)
                            }
                        }

                        itemsSeen && !documentSeen && resources.isEmpty() ->
                            bitwardenImporter.decodeParsed(BitwardenExport(encrypted, folders, items))

                        else -> throw BackupFailed()
                    }
                transferred = documentSeen
                return result
            } finally {
                document.wipe()
                if (!transferred) resources.values.forEach { it.fill(0) }
            }
        }

    }
