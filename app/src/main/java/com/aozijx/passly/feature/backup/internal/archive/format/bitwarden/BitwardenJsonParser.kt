package com.aozijx.passly.feature.backup.internal.archive.format.bitwarden

import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.io.LimitedOutputStream
import com.aozijx.passly.feature.backup.internal.archive.io.WipingByteArrayOutputStream
import com.aozijx.passly.feature.backup.internal.archive.io.copyCurrentValueTo
import com.aozijx.passly.feature.backup.internal.archive.io.forEachObjectField
import com.aozijx.passly.feature.backup.internal.archive.io.decodeStrictUtf8
import com.aozijx.passly.feature.backup.internal.archive.format.json.BackupJsonParserFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Reads Bitwarden root arrays incrementally and decodes one bounded record at a time. */
@Singleton
internal class BitwardenJsonParser
    @Inject
    constructor() {
        fun parse(input: InputStream): BitwardenExport {
            val folders = mutableListOf<BitwardenFolder>()
            val items = mutableListOf<BitwardenItem>()
            var encrypted = false
            var encryptedSeen = false
            var foldersSeen = false
            var itemsSeen = false
            var passlyDocumentSeen = false
            BackupJsonParserFactory.create(input).use { parser ->
                require(parser.nextToken() == JsonToken.START_OBJECT) { "Bitwarden JSON 根节点无效" }
                parser.forEachObjectField { name ->
                    when (name) {
                        "encrypted" -> {
                            require(!encryptedSeen) { "Bitwarden JSON 包含重复 encrypted 字段" }
                            encryptedSeen = true
                            val token = parser.currentToken
                            require(token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
                                "Bitwarden encrypted 字段无效"
                            }
                            encrypted = parser.booleanValue
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

                        "items" -> {
                            require(!itemsSeen) { "Bitwarden JSON 包含重复 items 字段" }
                            itemsSeen = true
                            require(parser.currentToken == JsonToken.START_ARRAY) { "Bitwarden items 无效" }
                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                require(items.size < BackupBundleValidator.MAX_ENTRIES) { "Bitwarden item 数量过多" }
                                require(parser.currentToken == JsonToken.START_OBJECT) { "Bitwarden item 无效" }
                                items += parser.decodeBitwardenRecord<BitwardenItem>()
                            }
                        }

                        "document" -> {
                            passlyDocumentSeen = true
                            parser.skipChildren()
                        }

                        else -> parser.skipChildren()
                    }
                }
                require(parser.currentToken == JsonToken.END_OBJECT && parser.nextToken() == null) {
                    "Bitwarden JSON 根对象无效"
                }
            }
            require(itemsSeen && !passlyDocumentSeen) { "JSON 根对象格式不明确" }
            return BitwardenExport(encrypted = encrypted, folders = folders, items = items)
        }

        internal companion object {
            const val MAX_RECORD_BYTES = 8 * 1024 * 1024
        }
    }
internal inline fun <reified T> JsonParser.decodeBitwardenRecord(): T {
    val output = WipingByteArrayOutputStream()
    try {
        JsonFactory().createGenerator(LimitedOutputStream(output, BitwardenJsonParser.MAX_RECORD_BYTES.toLong())).use { generator ->
            generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
            copyCurrentValueTo(generator)
        }
        val bytes = output.toByteArray()
        return try {
            BackupJson.decodeFromString<T>(bytes.decodeStrictUtf8("Bitwarden JSON record"))
        } finally {
            bytes.fill(0)
        }
    } finally {
        output.wipe()
    }
}
