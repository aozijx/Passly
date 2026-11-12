package com.aozijx.passly.feature.backup.internal.archive.format.json

import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.BackupImportPlanner
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.io.LimitedOutputStream
import com.aozijx.passly.feature.backup.internal.archive.io.WipingByteArrayOutputStream
import com.aozijx.passly.feature.backup.internal.archive.io.copyCurrentValueTo
import com.aozijx.passly.feature.backup.internal.archive.io.decodeStrictUtf8
import com.aozijx.passly.feature.backup.internal.archive.io.forEachObjectField
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Parses the root once and incrementally decodes each Base64 resource. */
@Singleton
internal class JsonBackupImporter
    @Inject
    constructor() {
        fun import(
            input: InputStream,
            strategy: BackupImportStrategy = BackupImportStrategy.STRICT,
        ): BackupBundle {
            val documentJson = WipingByteArrayOutputStream()
            val resources = LinkedHashMap<String, ByteArray>()
            var handedOff = false
            try {
                BackupJsonParserFactory.create(input).use { parser ->
                    require(parser.nextToken() == JsonToken.START_OBJECT) { "备份 JSON 根节点必须是对象" }
                    var documentSeen = false
                    var resourcesSeen = false
                    parser.forEachObjectField { name ->
                        when (name) {
                            "document" -> {
                                require(!documentSeen) { "备份包含重复 document" }
                                documentSeen = true
                                require(parser.currentToken == JsonToken.START_OBJECT) { "备份 document 无效" }
                                val bounded = LimitedOutputStream(documentJson, BackupBundleValidator.MAX_DOCUMENT_BYTES.toLong())
                                JsonFactory().createGenerator(bounded).use { generator ->
                                    generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                                    parser.copyCurrentValueTo(generator)
                                }
                            }

                            "resourcesBase64" -> {
                                require(!resourcesSeen) { "备份包含重复 resourcesBase64" }
                                resourcesSeen = true
                                require(parser.currentToken == JsonToken.START_OBJECT) { "备份资源对象无效" }
                                resources.putAll(parser.readBase64Resources())
                            }

                            "items" -> {
                                throw IllegalArgumentException("JSON 根对象格式不明确")
                            }

                            else -> {
                                parser.skipChildren()
                            }
                        }
                    }
                    require(parser.currentToken == JsonToken.END_OBJECT) { "备份 JSON 根对象未闭合" }
                    require(parser.nextToken() == null) { "备份 JSON 包含尾随内容" }
                    require(documentSeen) { "备份缺少 document" }
                }

                val bytes = documentJson.toByteArray()
                try {
                    val bundle = importParsed(bytes, resources, strategy)
                    handedOff = true
                    return bundle
                } finally {
                    bytes.fill(0)
                }
            } finally {
                documentJson.wipe()
                if (!handedOff) resources.values.forEach { it.fill(0) }
            }
        }

        internal fun importParsed(
            documentJson: ByteArray,
            resources: Map<String, ByteArray>,
            strategy: BackupImportStrategy,
        ): BackupBundle {
            val document = BackupJson.decodeFromString<BackupDocument>(documentJson.decodeStrictUtf8("备份 document"))
            return BackupImportPlanner.plan(BackupBundle(document, resources), strategy).bundle
        }
    }

internal fun com.fasterxml.jackson.core.JsonParser.readBase64Resources(): LinkedHashMap<String, ByteArray> {
    require(currentToken == JsonToken.START_OBJECT) { "备份资源对象无效" }
    val resources = LinkedHashMap<String, ByteArray>()
    var totalBytes = 0L
    try {
        forEachObjectField { id ->
            require(BackupBundleValidator.isSafeArchiveId(id)) { "备份资源 ID 无效" }
            require(resources.size < BackupBundleValidator.MAX_RESOURCES) { "备份资源数量过多" }
            require(id !in resources) { "备份包含重复资源 ID: $id" }
            require(currentToken == JsonToken.VALUE_STRING) { "备份资源 Base64 无效: $id" }
            val output = WipingByteArrayOutputStream()
            try {
                readBinaryValue(
                    com.fasterxml.jackson.core.Base64Variants
                        .getDefaultVariant(),
                    LimitedOutputStream(output, BackupBundleValidator.MAX_RESOURCE_BYTES.toLong()),
                )
                val bytes = output.toByteArray()
                totalBytes = Math.addExact(totalBytes, bytes.size.toLong())
                require(totalBytes <= BackupBundleValidator.MAX_TOTAL_RESOURCE_BYTES) {
                    bytes.fill(0)
                    "备份资源总量过大"
                }
                resources[id] = bytes
            } finally {
                output.wipe()
            }
        }
        return resources
    } catch (error: Throwable) {
        resources.values.forEach { it.fill(0) }
        throw error
    }
}
