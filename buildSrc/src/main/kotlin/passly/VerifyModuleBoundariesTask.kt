package passly

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyModuleBoundariesTask : DefaultTask() {
    @get:Input
    abstract val actualModules: SetProperty<String>

    @get:Input
    abstract val actualEdges: SetProperty<String>

    @get:Input
    abstract val policyModules: SetProperty<String>

    @get:Input
    abstract val allowedEdges: SetProperty<String>

    @get:Input
    abstract val requiredEdges: SetProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Internal
    abstract val sourceRoot: DirectoryProperty

    @TaskAction
    fun verify() {
        val modules = actualModules.get()
        val policies = policyModules.get()
        val missingPolicies = missingPolicyModules(modules, policies)
        if (missingPolicies.isNotEmpty()) {
            throw GradleException(
                "Missing module dependency policies: ${missingPolicies.sorted()}",
            )
        }

        val stalePolicies = stalePolicyModules(modules, policies)
        if (stalePolicies.isNotEmpty()) {
            throw GradleException(
                "Policies reference missing modules: ${stalePolicies.sorted()}",
            )
        }

        val unknownAllowedModules = allowedEdges.get()
            .flatMap { value -> value.split(EDGE_SEPARATOR, limit = 2) }
            .toSet() - modules
        if (unknownAllowedModules.isNotEmpty()) {
            throw GradleException(
                "Allowed edges reference missing modules: ${unknownAllowedModules.sorted()}",
            )
        }

        val forbiddenEdges = actualEdges.get() - allowedEdges.get()
        if (forbiddenEdges.isNotEmpty()) {
            throw GradleException(
                "Forbidden project dependencies:\n${forbiddenEdges.sorted().joinToString("\n")}",
            )
        }

        val cycle = findCycle(actualEdges.get())
        if (cycle != null) {
            throw GradleException("Cyclic project dependency: ${cycle.joinToString(" -> ")}")
        }

        val missingRequiredEdges = missingRequiredEdges(
            required = requiredEdges.get(),
            actual = actualEdges.get(),
        )
        if (missingRequiredEdges.isNotEmpty()) {
            throw GradleException(
                "Missing required direct project dependencies:\n${missingRequiredEdges.sorted().joinToString("\n")}",
            )
        }

        val root = sourceRoot.get().asFile.toPath()
        val sources = sourceFiles.files.map { file ->
            EditorSource(
                path = root.relativize(file.toPath()).toString(),
                content = file.readText(),
            )
        }
        val sourceViolations = EditorSourceBoundaryVerifier.verify(sources)
        if (sourceViolations.isNotEmpty()) {
            throw GradleException(
                "Forbidden source dependencies:\n${sourceViolations.sorted().joinToString("\n")}",
            )
        }

        val presentationApiSignals = PresentationUiApiVerifier.inspect(sources)
        if (presentationApiSignals.isNotEmpty()) {
            throw GradleException(
                "Oversized Presentation UI APIs:\n${presentationApiSignals.joinToString("\n")}",
            )
        }
    }

    private fun findCycle(edges: Set<String>): List<String>? {
        val dependencies = edges
            .map { value -> value.split(EDGE_SEPARATOR, limit = 2) }
            .groupBy(keySelector = List<String>::first, valueTransform = List<String>::last)
        val visited = mutableSetOf<String>()
        val active = mutableListOf<String>()

        fun visit(module: String): List<String>? {
            val activeIndex = active.indexOf(module)
            if (activeIndex >= 0) return active.drop(activeIndex) + module
            if (!visited.add(module)) return null

            active += module
            dependencies[module].orEmpty().forEach { dependency ->
                visit(dependency)?.let { return it }
            }
            active.removeAt(active.lastIndex)
            return null
        }

        return actualModules.get().sorted().firstNotNullOfOrNull(::visit)
    }
}

internal fun missingRequiredEdges(required: Set<String>, actual: Set<String>): Set<String> =
    required - actual

internal fun missingPolicyModules(actual: Set<String>, policies: Set<String>): Set<String> =
    actual - policies

internal fun stalePolicyModules(actual: Set<String>, policies: Set<String>): Set<String> =
    policies - actual

internal fun architectureSourcePatterns(modules: Set<String>): Set<String> =
    modules.mapTo(linkedSetOf()) { module ->
        "${module.removePrefix(":").replace(':', '/')}/src/**/*.kt"
    }
