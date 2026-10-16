package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

internal data class CanonicalDiagnosticsEvent(
    private val record: SanitizedLogRecord,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("time", isoTime(record.timestampMs))
        put("timestamp_ms", record.timestampMs)
        put("elapsed_realtime_ms", record.elapsedRealtimeMs)
        put("level", record.level.name)
        put("event", record.event.value)
        put("event_id", record.eventId.value)
        put("context", buildJsonObject {
            put("category", record.category.name)
            orderedFields(record).forEach { (key, value) -> put(key, value.toJson()) }
        })
        record.error?.let { error ->
            put("error", buildJsonObject {
                put("error_code", error.errorCode.value)
                put("reason_code", error.reason.value)
                error.exception?.let { exception ->
                    put("exception", buildJsonObject {
                        put("kind", exception.kind.name)
                        put("type", exception.type.value)
                        put("app_stack", JsonPrimitive(exception.appStackFrames.joinToString(",") { it.value }))
                        put("cause_kinds", JsonPrimitive(exception.causeTypes.joinToString(",") { it.value }))
                        put("foreign_frame_count", exception.foreignFrameCount)
                    })
                }
            })
        }
        put("trace", buildJsonObject {
            put("trace_id", record.trace.traceId.value)
            put("span_id", record.trace.spanId.value)
            record.trace.parentSpanId?.let { put("parent_span_id", it.value) }
        })
        put("session", buildJsonObject {
            put("process_session_id", record.processSessionId.value)
            record.secureSessionId?.let { put("secure_session_id", it.value) }
        })
    }

    companion object {
        fun from(record: SanitizedLogRecord): CanonicalDiagnosticsEvent = CanonicalDiagnosticsEvent(record)

        private val preferredKeys = listOf(
            LogFieldKeys.Result.value,
            "purpose",
            LogFieldKeys.Trigger.value,
            "reason_code",
            LogFieldKeys.ElapsedMs.value,
            LogFieldKeys.TimeoutMs.value,
            LogFieldKeys.RetryCount.value,
        )

        private fun orderedFields(record: SanitizedLogRecord): List<Pair<String, LogValue>> {
            val values = record.fields.values.entries.associate { it.key.value to it.value }
            return preferredKeys.mapNotNull { key -> values[key]?.let { key to it } } +
                values.filterKeys { it !in preferredKeys }.toSortedMap().map { it.key to it.value }
        }

        private fun LogValue.toJson(): JsonPrimitive = when (this) {
            is LogValue.Count -> JsonPrimitive(value)
            is LogValue.DurationMs -> JsonPrimitive(value)
            is LogValue.Ratio -> JsonPrimitive(value)
            is LogValue.BooleanValue -> JsonPrimitive(value)
            is LogValue.EnumValue -> JsonPrimitive(value.value)
            is LogValue.ErrorCodeValue -> JsonPrimitive(value.value)
            is LogValue.ReasonCodeValue -> JsonPrimitive(value.value)
            is LogValue.OperationCodeValue -> JsonPrimitive(value.value)
            is LogValue.HashedIdentifierValue -> JsonPrimitive(value.value)
            is LogValue.HashedIdentifierInput -> error("Pending identifier reached diagnostics export")
        }
    }
}

internal class JsonEventToTsvProjector {
    fun project(event: JsonObject): String {
        val context = event.objectOrEmpty("context")
        val error = event.objectOrEmpty("error")
        val exception = error.objectOrEmpty("exception")
        val trace = event.objectOrEmpty("trace")
        val session = event.objectOrEmpty("session")
        val details = linkedMapOf<String, String>()
        context.entries.filter { it.key != "result" }.forEach { (key, value) -> details[key] = value.text() }
        event.entries.filter { it.key !in KNOWN_EVENT_KEYS }.forEach { (key, value) -> flatten(key, value, details) }
        val errorValues = linkedMapOf<String, String>()
        error["error_code"]?.let { errorValues["error_code"] = it.text() }
        error["reason_code"]?.let { errorValues["reason_code"] = it.text() }
        val exceptionValues = linkedMapOf<String, String>()
        exception.forEach { (key, value) -> exceptionValues[key] = value.text() }
        val traceValues = linkedMapOf<String, String>()
        listOf("trace_id", "span_id", "parent_span_id").forEach { key ->
            trace[key]?.let { traceValues[key] = it.text() }
        }
        val sessionValues = linkedMapOf<String, String>()
        listOf("process_session_id", "secure_session_id").forEach { key ->
            session[key]?.let { sessionValues[key] = it.text() }
        }
        return listOf(
            event["time"]?.text().orEmpty(),
            event["level"]?.text().orEmpty(),
            event["event"]?.text().orEmpty(),
            context["result"]?.text().orEmpty(),
            details.asBag(),
            (errorValues + exceptionValues.mapKeys { "exception.${it.key}" }).asBag(),
            traceValues.asBag(),
            sessionValues.asBag(),
        ).joinToString("\t") { it.escapeTsv() }
    }

    private fun flatten(prefix: String, value: JsonElement, output: MutableMap<String, String>) {
        if (value is JsonObject) {
            value.forEach { (key, child) -> flatten("$prefix.$key", child, output) }
        } else {
            output[prefix] = value.text()
        }
    }

    private fun Map<String, String>.asBag(): String = entries.joinToString(";") { (key, value) ->
        "${key.escapeBag()}=${value.escapeBag()}"
    }

    private fun String.escapeBag(): String = replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace("=", "\\=")
        .replace("\t", "\\t")
        .replace("\r", "\\r")
        .replace("\n", "\\n")

    private fun String.escapeTsv(): String = replace("\t", "\\t")
        .replace("\r", "\\r")
        .replace("\n", "\\n")

    private fun JsonObject.objectOrEmpty(key: String): JsonObject = this[key] as? JsonObject ?: JsonObject(emptyMap())

    private fun JsonElement.text(): String = when (this) {
        is JsonPrimitive -> content
        JsonNull -> ""
        else -> Json.encodeToString(JsonElement.serializer(), this)
    }

    private companion object {
        val KNOWN_EVENT_KEYS = setOf(
            "time", "timestamp_ms", "elapsed_realtime_ms", "level", "event", "event_id",
            "context", "error", "trace", "session",
        )
    }
}

internal class TsvRecordEncoder @Inject constructor() {
    private val projector = JsonEventToTsvProjector()

    fun encode(record: SanitizedLogRecord): String =
        projector.project(CanonicalDiagnosticsEvent.from(record).toJson())
}

internal class DiagnosticsExportEncoder @Inject constructor() {
    private val projector = JsonEventToTsvProjector()
    fun write(
        format: LogFormat,
        output: OutputStream,
        manifest: ExportManifest,
        records: List<SanitizedLogRecord>,
        embedManifest: Boolean = true,
    ) {
        val events = records.sortedWith(
            compareBy<SanitizedLogRecord> { it.timestampMs }
                .thenBy { it.elapsedRealtimeMs }
                .thenBy { it.eventId.value },
        ).map { CanonicalDiagnosticsEvent.from(it).toJson() }
        when (format) {
            LogFormat.JSONL -> writeJsonl(output, manifest, events, embedManifest)
            LogFormat.TSV -> writeTsv(output, manifest, events, embedManifest)
            LogFormat.ZIP -> writeZip(output, manifest, events)
        }
    }

    private fun writeJsonl(output: OutputStream, manifest: ExportManifest, events: List<JsonObject>, embedManifest: Boolean) {
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            if (embedManifest) writer.appendLine(Json.encodeToString(JsonElement.serializer(), manifest.toJson()))
            events.forEach { writer.appendLine(Json.encodeToString(JsonElement.serializer(), it)) }
        }
    }

    private fun writeTsv(output: OutputStream, manifest: ExportManifest, events: List<JsonObject>, embedManifest: Boolean) {
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            if (embedManifest) writer.appendLine(manifest.toTsvHeader())
            writer.appendLine("time\tlevel\tevent\toutcome\tdetails\texception\ttrace\tsession")
            events.forEach { writer.appendLine(projector.project(it)) }
        }
    }

    private fun writeZip(output: OutputStream, manifest: ExportManifest, events: List<JsonObject>) {
        ZipOutputStream(output).use { zip ->
            zip.writeEntry("manifest.json", Json.encodeToString(JsonElement.serializer(), manifest.toJson()) + "\n")
            zip.writeEntry("logs.jsonl") { writeJsonl(it, manifest, events, false) }
            zip.writeEntry("logs.tsv") { writeTsv(it, manifest, events, false) }
            zip.writeEntry("README.txt", ZIP_README)
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        writeEntry(name) { it.write(content.encodeToByteArray()) }
    }

    private fun ZipOutputStream.writeEntry(name: String, writer: (OutputStream) -> Unit) {
        putNextEntry(ZipEntry(name).apply { time = 0L })
        writer(NonClosingOutputStream(this))
        closeEntry()
    }

    private class NonClosingOutputStream(private val delegate: OutputStream) : OutputStream() {
        override fun write(value: Int) = delegate.write(value)
        override fun write(bytes: ByteArray, offset: Int, length: Int) = delegate.write(bytes, offset, length)
        override fun flush() = delegate.flush()
        override fun close() = flush()
    }

    private companion object {
        const val ZIP_README = "Passly logs. JSONL is the canonical machine format; TSV is the human-readable projection.\n"
    }
}

private fun ExportManifest.toJson(): JsonObject = buildJsonObject {
    put("type", "manifest")
    put("schema_version", schemaVersion)
    put("generated_at", isoTime(generatedAtMs))
    put("generated_at_ms", generatedAtMs)
    put("record_count", recordCount)
    put("app_name", appName)
    put("app_version_name", appVersionName)
    put("app_version_code", appVersionCode)
    put("manufacturer", manufacturer)
    put("model", model)
    put("sdk_int", sdkInt)
    put("os_release", osRelease)
    put("locales", JsonPrimitive(locales.joinToString(",")))
    put("timezone", timezone)
}

private fun ExportManifest.toTsvHeader(): String = listOf(
    "schema_version=$schemaVersion",
    "generated_at=${isoTime(generatedAtMs)}",
    "record_count=$recordCount",
    "app_name=$appName",
    "app_version_name=$appVersionName",
    "app_version_code=$appVersionCode",
    "manufacturer=$manufacturer",
    "model=$model",
    "sdk_int=$sdkInt",
    "os_release=$osRelease",
    "locales=${locales.joinToString(",")}",
    "timezone=$timezone",
).joinToString("\t", prefix = "# ") { value ->
    value.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n")
}

private fun isoTime(timestampMs: Long): String = Instant.ofEpochMilli(timestampMs).toString()
