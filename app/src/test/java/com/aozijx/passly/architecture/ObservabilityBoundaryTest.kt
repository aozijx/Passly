package com.aozijx.passly.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservabilityBoundaryTest {
    @Test
    fun `production has one observability path and one Android Log owner`() {
        val violations = productionSources().flatMap { file ->
            val text = file.readText()
            buildList {
                FORBIDDEN.forEach { token ->
                    if (token in text) add("${file.invariantSeparatorsPath}: $token")
                }
                if (
                    "android.util.Log" in text &&
                    file.name != "LogcatSink.kt"
                ) {
                    add("${file.invariantSeparatorsPath}: android.util.Log")
                }
            }
        }.toList()

        assertTrue(violations.joinToString(separator = "\n"), violations.isEmpty())
    }

    private fun productionSources(): Sequence<File> {
        val root = listOf(File("."), File("..")).firstOrNull {
            File(it, "app/src/main").isDirectory
        }?.canonicalFile ?: error("Cannot locate repository root")
        return SOURCE_ROOTS.asSequence()
            .map { File(root, it) }
            .filter(File::isDirectory)
            .flatMap { directory -> directory.walkTopDown().filter { it.isFile && it.extension == "kt" } }
    }

    private companion object {
        val SOURCE_ROOTS = listOf(
            "app/src/main",
            "core/src/main",
            "data/src/main",
            "security/src/main",
            "runtime/session/src/main",
            "domain/src/main",
        )
        val FORBIDDEN = listOf(
            "TelemetryRuntime",
            "TelemetryReporter",
            "TelemetryEvent",
            "printStackTrace",
            "Log.getStackTraceString",
            "diagnostics_v1",
            ".elog1",
        )
    }
}
