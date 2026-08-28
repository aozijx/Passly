package passly

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

internal data class ModuleFileCount(
    val production: Int = 0,
    val test: Int = 0,
)

internal data class ArchitectureMetricsReport(
    val moduleFileCounts: Map<String, ModuleFileCount>,
    val appOwnershipCounts: Map<String, Int>,
    val invariantImportCounts: Map<String, Int>,
    val reviewSignals: List<String>,
) {
    fun render(): String = buildString {
        appendLine("Kotlin files by module/source-set:")
        moduleFileCounts.toSortedMap().forEach { (module, count) ->
            appendLine("  $module main=${count.production} test=${count.test}")
        }
        appendLine("Major App ownership areas:")
        appOwnershipCounts.toSortedMap().forEach { (area, count) ->
            appendLine("  $area=$count")
        }
        appendLine("Direct import invariants:")
        invariantImportCounts.toSortedMap().forEach { (edge, count) ->
            appendLine("  $edge=$count")
        }
        appendLine("Review signals:")
        reviewSignals.sorted().forEach { appendLine("  $it") }
    }.trimEnd()
}

internal object ArchitectureMetrics {
    private const val LARGE_FILE_LINES = 300
    private const val COMPOSABLE_PARAMETER_THRESHOLD = 8

    fun analyze(sources: List<EditorSource>): ArchitectureMetricsReport {
        val normalized = sources.map { it.copy(path = it.path.replace('\\', '/')) }
        val moduleCounts = normalized.groupBy { moduleOf(it.path) }
            .mapValues { (_, files) ->
                ModuleFileCount(
                    production = files.count { !it.path.isTestSource() },
                    test = files.count { it.path.isTestSource() },
                )
            }
        val production = normalized.filterNot { it.path.isTestSource() }
        val appOwnership = production.asSequence()
            .filter { it.path.startsWith("app/src/") }
            .mapNotNull { source -> appOwnershipOf(source.path) }
            .groupingBy(String::toString)
            .eachCount()
        val imports = production.associateWith { source ->
            source.content.lineSequence()
                .map(String::trim)
                .filter { it.startsWith("import ") }
                .map { it.removePrefix("import ").substringBefore(" as ") }
                .toList()
        }
        val invariantCounts = sortedMapOf(
            "data->app/feature/presentation" to imports.countImports(
                owner = { it.startsWith("data/src/") },
                forbidden = { imported ->
                    listOf("app", "feature", "presentation")
                        .any { layer -> imported.isProjectLayerImport(layer) }
                },
            ),
            "domain->android" to imports.countImports(
                owner = { it.startsWith("domain/src/") },
                forbidden = { it.startsWith("android.") || it.startsWith("androidx.") },
            ),
            "feature->presentation" to imports.countImports(
                owner = { "/feature/" in it && "/presentation/feature/" !in it },
                forbidden = { it.isProjectLayerImport("presentation") },
            ),
            "presentation->data" to imports.countImports(
                owner = { "/presentation/" in it },
                forbidden = { it.isProjectLayerImport("data") },
            ),
        )

        val testFileNames = normalized.filter { it.path.isTestSource() }
            .mapTo(hashSetOf()) { it.path.substringAfterLast('/').lowercase() }
        val signals = buildList {
            production.forEach { source ->
                val fileName = source.path.substringAfterLast('/')
                val lineCount = source.content.lineSequence().count()
                if (lineCount > LARGE_FILE_LINES) {
                    add("${source.path}: $lineCount lines")
                }
                if (
                    "/presentation/feature/" in source.path &&
                    listOf("Screen.kt", "Content.kt", "Component.kt", "Dialog.kt", "Sheet.kt")
                        .any(fileName::endsWith)
                ) {
                    add("${source.path}: passive UI below presentation.feature")
                }
                if (fileName.endsWith("ViewModel.kt")) {
                    val flowStem = fileName.removeSuffix("ViewModel.kt").lowercase()
                    if (testFileNames.none { it.startsWith(flowStem) }) {
                        add("${source.path}: ViewModel without focused test")
                    }
                }
                if (
                    fileName.endsWith("Reducer.kt") &&
                    Regex("\\bfun\\s+").findAll(source.content).count() == 1 &&
                    !Regex("\\bwhen\\s*\\(").containsMatchIn(source.content)
                ) {
                    add("${source.path}: trivial reducer")
                }
                composableParameterCounts(source.content)
                    .filter { it > COMPOSABLE_PARAMETER_THRESHOLD }
                    .forEach { parameterCount ->
                        add(
                            "${source.path}: composable has $parameterCount parameters " +
                                "(threshold $COMPOSABLE_PARAMETER_THRESHOLD)",
                        )
                    }
            }
        }.sorted()

        return ArchitectureMetricsReport(
            moduleFileCounts = moduleCounts,
            appOwnershipCounts = appOwnership,
            invariantImportCounts = invariantCounts,
            reviewSignals = signals,
        )
    }

    private fun moduleOf(path: String): String = when {
        path.startsWith("core/common/") -> ":core:common"
        path.startsWith("runtime/session/") -> ":runtime:session"
        else -> ":${path.substringBefore('/')}"
    }

    private fun appOwnershipOf(path: String): String? = when {
        "/com/aozijx/passly/core/" in path -> "app-local-core"
        "/presentation/feature/" in path -> "presentation.feature"
        "/presentation/ui/" in path -> "presentation.ui"
        "/feature/" in path -> "feature"
        "/security/" in path -> "security"
        "/app/" in path -> "app"
        else -> "other"
    }

    private fun String.isTestSource(): Boolean =
        "/src/test/" in this || "/src/androidTest/" in this

    private fun Map<EditorSource, List<String>>.countImports(
        owner: (String) -> Boolean,
        forbidden: (String) -> Boolean,
    ): Int = entries.asSequence()
        .filter { owner(it.key.path) }
        .sumOf { (_, imports) -> imports.count(forbidden) }

    private fun String.isProjectLayerImport(layer: String): Boolean =
        startsWith("com.aozijx.passly.$layer.") || startsWith("com.example.$layer.")

    private fun composableParameterCounts(content: String): List<Int> {
        val counts = mutableListOf<Int>()
        var searchFrom = 0
        while (true) {
            val annotation = content.indexOf("@Composable", searchFrom)
            if (annotation < 0) break
            val function = content.indexOf("fun ", annotation)
            if (function < 0) break
            val visibilityPrefix = content.substring(annotation, function)
            if ("private " !in visibilityPrefix) {
                val open = content.indexOf('(', function)
                if (open >= 0) {
                    val close = matchingParenthesis(content, open)
                    if (close > open) counts += topLevelParameterCount(content.substring(open + 1, close))
                }
            }
            searchFrom = function + 4
        }
        return counts
    }

    private fun matchingParenthesis(content: String, open: Int): Int {
        var depth = 0
        for (index in open until content.length) {
            when (content[index]) {
                '(' -> depth++
                ')' -> if (--depth == 0) return index
            }
        }
        return -1
    }

    private fun topLevelParameterCount(parameters: String): Int {
        if (parameters.isBlank()) return 0
        var nesting = 0
        var count = 1
        parameters.forEach { character ->
            when (character) {
                '(', '<', '[', '{' -> nesting++
                ')', '>', ']', '}' -> nesting--
                ',' -> if (nesting == 0) count++
            }
        }
        return count
    }
}

abstract class ArchitectureMetricsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Internal
    abstract val sourceRoot: DirectoryProperty

    @get:OutputFile
    abstract val outputReport: RegularFileProperty

    @TaskAction
    fun report() {
        val root = sourceRoot.get().asFile.toPath()
        val metrics = ArchitectureMetrics.analyze(
            sourceFiles.files.map { file ->
                EditorSource(
                    path = root.relativize(file.toPath()).toString(),
                    content = file.readText(),
                )
            },
        )
        val rendered = metrics.render()
        val reportFile: File = outputReport.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(rendered + System.lineSeparator())
        logger.lifecycle(rendered)

        val violations = metrics.invariantImportCounts.filterValues { it > 0 }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Architecture import invariants regressed: " +
                    violations.toSortedMap().entries.joinToString { "${it.key}=${it.value}" },
            )
        }
    }
}
