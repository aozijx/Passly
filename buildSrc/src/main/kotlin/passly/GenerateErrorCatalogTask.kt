package passly

import groovy.json.JsonSlurper
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal data class ErrorCatalog(
    val schemaVersion: Int,
    val reasons: List<ReasonCatalogEntry>,
    val errors: List<ErrorCatalogEntry>,
)

internal data class ReasonCatalogEntry(
    val symbol: String,
    val code: String,
    val description: String,
)

internal data class ErrorCatalogEntry(
    val symbol: String,
    val code: String,
    val defaultReason: String,
    val category: String,
    val exposure: String,
    val description: String,
    val messageKey: String?,
)

internal object ErrorCatalogParser {
    private val symbolPattern = Regex("^[A-Z][A-Za-z0-9]{1,63}$")
    private val codePattern = Regex("^[A-Z][A-Z0-9_]{2,63}$")
    private val messageKeyPattern = Regex("^[a-z][a-z0-9_]{2,127}$")
    private val categories = setOf("APPLICATION", "DATABASE", "SECURITY", "AUTOFILL", "BACKUP", "VAULT")
    private val exposures = setOf("INTERNAL", "USER_ACTIONABLE")

    fun parse(file: File): ErrorCatalog {
        val root = JsonSlurper().parse(file).asStringMap("catalog")
        root.requireKeys(setOf("schema_version", "reasons", "errors"), "catalog")
        val schemaVersion = root.requiredNumber("schema_version").toInt()
        require(schemaVersion == 1) { "Unsupported error catalog schema $schemaVersion" }
        val reasons = root.requiredList("reasons").mapIndexed { index, value ->
            val item = value.asStringMap("reasons[$index]")
            item.requireKeys(setOf("symbol", "code", "description"), "reasons[$index]")
            ReasonCatalogEntry(
                symbol = item.requiredString("symbol"),
                code = item.requiredString("code"),
                description = item.requiredString("description"),
            )
        }
        val errors = root.requiredList("errors").mapIndexed { index, value ->
            val item = value.asStringMap("errors[$index]")
            item.requireKeys(
                setOf("symbol", "code", "default_reason", "category", "exposure", "description", "message_key"),
                "errors[$index]",
                optional = setOf("message_key"),
            )
            ErrorCatalogEntry(
                symbol = item.requiredString("symbol"),
                code = item.requiredString("code"),
                defaultReason = item.requiredString("default_reason"),
                category = item.requiredString("category"),
                exposure = item.requiredString("exposure"),
                description = item.requiredString("description"),
                messageKey = item["message_key"]?.let {
                    require(it is String && it.isNotBlank()) { "message_key must be a non-empty string" }
                    it
                },
            )
        }
        validate(reasons, errors)
        return ErrorCatalog(schemaVersion, reasons, errors)
    }

    private fun validate(reasons: List<ReasonCatalogEntry>, errors: List<ErrorCatalogEntry>) {
        require(reasons.isNotEmpty()) { "At least one reason is required" }
        require(errors.isNotEmpty()) { "At least one error is required" }
        reasons.forEach {
            require(symbolPattern.matches(it.symbol)) { "Invalid reason symbol ${it.symbol}" }
            require(codePattern.matches(it.code)) { "Invalid reason code ${it.code}" }
            require(it.description.isNotBlank()) { "Reason description cannot be blank" }
        }
        errors.forEach {
            require(symbolPattern.matches(it.symbol)) { "Invalid error symbol ${it.symbol}" }
            require(codePattern.matches(it.code)) { "Invalid error code ${it.code}" }
            require(it.category in categories) { "Invalid error category ${it.category}" }
            require(it.exposure in exposures) { "Invalid error exposure ${it.exposure}" }
            require(it.description.isNotBlank()) { "Error description cannot be blank" }
            require(it.defaultReason in reasons.map(ReasonCatalogEntry::code)) {
                "Unknown default reason ${it.defaultReason} for ${it.code}"
            }
            when (it.exposure) {
                "USER_ACTIONABLE" -> requireNotNull(it.messageKey) {
                    "USER_ACTIONABLE error ${it.code} requires message_key"
                }
                "INTERNAL" -> require(it.messageKey == null) {
                    "INTERNAL error ${it.code} cannot declare message_key"
                }
            }
            it.messageKey?.let { key ->
                require(messageKeyPattern.matches(key)) { "Invalid message_key $key" }
            }
        }
        requireUnique("reason symbols", reasons.map(ReasonCatalogEntry::symbol))
        requireUnique("reason codes", reasons.map(ReasonCatalogEntry::code))
        requireUnique("error symbols", errors.map(ErrorCatalogEntry::symbol))
        requireUnique("error codes", errors.map(ErrorCatalogEntry::code))
    }

    private fun requireUnique(label: String, values: List<String>) {
        val duplicates = values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate $label: ${duplicates.sorted().joinToString()}" }
    }

    private fun Any?.asStringMap(label: String): Map<String, Any?> {
        require(this is Map<*, *>) { "$label must be an object" }
        return entries.associate { (key, value) ->
            require(key is String) { "$label contains a non-string key" }
            key to value
        }
    }

    private fun Map<String, Any?>.requiredList(key: String): List<*> =
        get(key).also { require(it is List<*>) { "$key must be an array" } } as List<*>

    private fun Map<String, Any?>.requiredString(key: String): String =
        get(key).also { require(it is String && it.isNotBlank()) { "$key must be a non-empty string" } } as String

    private fun Map<String, Any?>.requiredNumber(key: String): Number =
        get(key).also { require(it is Number) { "$key must be a number" } } as Number

    private fun Map<String, Any?>.requireKeys(
        allowed: Set<String>,
        label: String,
        optional: Set<String> = emptySet(),
    ) {
        val unknown = keys - allowed
        require(unknown.isEmpty()) { "$label contains unknown properties: ${unknown.sorted().joinToString()}" }
        val missing = allowed - optional - keys
        require(missing.isEmpty()) { "$label is missing properties: ${missing.sorted().joinToString()}" }
    }
}

internal data class RenderedErrorCatalog(
    val coreKotlin: String,
    val appKotlin: String,
    val markdown: String,
)

internal object ErrorCatalogRenderer {
    fun render(catalog: ErrorCatalog): RenderedErrorCatalog = RenderedErrorCatalog(
        coreKotlin = renderCore(catalog),
        appKotlin = renderApp(catalog),
        markdown = renderMarkdown(catalog),
    )

    private fun renderCore(catalog: ErrorCatalog): String = buildString {
        appendLine("// Generated from config/observability/error-catalog.json. Do not edit.")
        appendLine("package com.aozijx.passly.core.observability")
        appendLine()
        appendLine("enum class ErrorExposure { INTERNAL, USER_ACTIONABLE }")
        appendLine()
        appendLine("data class RegisteredError(")
        appendLine("    val code: ErrorCode,")
        appendLine("    val defaultReason: ReasonCode,")
        appendLine("    val category: LogCategory,")
        appendLine("    val exposure: ErrorExposure,")
        appendLine(")")
        appendLine()
        appendLine("object ErrorCodes {")
        catalog.errors.groupBy(ErrorCatalogEntry::category).forEach { (category, errors) ->
            appendLine("    object ${category.categoryObjectName()} {")
            errors.forEach { appendLine("        val ${it.symbol} = ErrorCode(\"${it.code}\")") }
            appendLine("    }")
        }
        appendLine("}")
        appendLine()
        appendLine("object Reasons {")
        catalog.reasons.forEach { appendLine("    val ${it.symbol} = ReasonCode(\"${it.code}\")") }
        appendLine("}")
        appendLine()
        appendLine("object ErrorCatalog {")
        appendLine("    val All: List<RegisteredError> = listOf(")
        catalog.errors.forEach {
            val reasonSymbol = catalog.reasons.single { reason -> reason.code == it.defaultReason }.symbol
            appendLine("        RegisteredError(ErrorCodes.${it.category.categoryObjectName()}.${it.symbol}, Reasons.$reasonSymbol, LogCategory.${it.category}, ErrorExposure.${it.exposure}),")
        }
        appendLine("    )")
        appendLine("}")
    }

    private fun renderApp(catalog: ErrorCatalog): String = buildString {
        appendLine("// Generated from config/observability/error-catalog.json. Do not edit.")
        appendLine("package com.aozijx.passly.app.message.presentation")
        appendLine()
        appendLine("import androidx.annotation.StringRes")
        appendLine("import com.aozijx.passly.R")
        appendLine("import com.aozijx.passly.core.observability.ErrorCode")
        appendLine("import com.aozijx.passly.core.observability.ErrorCodes")
        appendLine()
        appendLine("object GeneratedErrorMessageResolver {")
        appendLine("    @StringRes")
        appendLine("    fun resolve(errorCode: ErrorCode): Int? = when (errorCode) {")
        catalog.errors.filter { it.messageKey != null }.forEach {
            appendLine("        ErrorCodes.${it.category.categoryObjectName()}.${it.symbol} -> R.string.${it.messageKey}")
        }
        appendLine("        else -> null")
        appendLine("    }")
        appendLine("}")
    }

    private fun renderMarkdown(catalog: ErrorCatalog): String = buildString {
        appendLine("# Passly Error Codes")
        appendLine()
        appendLine("> Generated from `config/observability/error-catalog.json`. Do not edit by hand.")
        appendLine()
        appendLine("## Errors by category")
        catalog.errors.groupBy(ErrorCatalogEntry::category).forEach { (category, errors) ->
            appendLine()
            appendLine("### ${category.categoryObjectName()}")
            appendLine()
            appendLine("| Code | Default reason | Exposure | Description |")
            appendLine("|---|---|---|---|")
            errors.forEach {
                appendLine("| `${it.code}` | `${it.defaultReason}` | ${it.exposure} | ${it.description} |")
            }
        }
        appendLine()
        appendLine("## Reasons")
        appendLine()
        appendLine("| Code | Description |")
        appendLine("|---|---|")
        catalog.reasons.forEach { appendLine("| `${it.code}` | ${it.description} |") }
    }

    private fun String.categoryObjectName(): String = lowercase().replaceFirstChar(Char::uppercase)
}

abstract class GenerateErrorCatalogTask : DefaultTask() {
    @get:InputFile
    abstract val catalogFile: RegularFileProperty

    @get:Optional
    @get:org.gradle.api.tasks.OutputDirectory
    abstract val coreOutputDirectory: DirectoryProperty

    @get:Optional
    @get:org.gradle.api.tasks.OutputDirectory
    abstract val appOutputDirectory: DirectoryProperty

    @get:Optional
    @get:OutputFile
    abstract val markdownOutputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val rendered = ErrorCatalogRenderer.render(ErrorCatalogParser.parse(catalogFile.get().asFile))
        coreOutputDirectory.orNull?.asFile
            ?.resolve("com/aozijx/passly/core/observability/GeneratedErrorCatalog.kt")
            ?.writeGenerated(rendered.coreKotlin)
        appOutputDirectory.orNull?.asFile
            ?.resolve("com/aozijx/passly/app/message/presentation/GeneratedErrorMessageResolver.kt")
            ?.writeGenerated(rendered.appKotlin)
        markdownOutputFile.orNull?.asFile?.writeGenerated(rendered.markdown)
    }

    private fun File.writeGenerated(content: String) {
        parentFile.mkdirs()
        writeText(content)
    }
}
