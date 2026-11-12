package com.aozijx.passly.feature.backup.internal.archive.format.json

import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.fasterxml.jackson.core.Base64Variants
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import java.io.ByteArrayInputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Streams resource Base64 while serializing the bounded structured document once. */
@Singleton
internal class JsonBackupExporter
    @Inject
    constructor() {
        fun export(
            bundle: BackupBundle,
            output: OutputStream,
        ) {
            BackupBundleValidator.validate(bundle, requireResourceData = bundle.document.resources.isNotEmpty())
            val document = BackupJson.encodeToString(bundle.document)
            val documentBytes = document.toByteArray(Charsets.UTF_8)
            try {
                require(documentBytes.size <= BackupBundleValidator.MAX_DOCUMENT_BYTES) { "备份文档过大" }
            } finally {
                documentBytes.fill(0)
            }

            val generator = JsonFactory().createGenerator(output)
            generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
            generator.use { json ->
                json.writeStartObject()
                json.writeFieldName("document")
                json.writeRawValue(document)
                if (bundle.resourceData.isNotEmpty()) {
                    json.writeObjectFieldStart("resourcesBase64")
                    bundle.resourceData.toSortedMap().forEach { (id, content) ->
                        json.writeFieldName(id)
                        json.writeBinary(Base64Variants.getDefaultVariant(), ByteArrayInputStream(content), content.size)
                    }
                    json.writeEndObject()
                }
                json.writeEndObject()
            }
        }
    }
