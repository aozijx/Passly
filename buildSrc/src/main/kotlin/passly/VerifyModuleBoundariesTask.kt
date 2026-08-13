package passly

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
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

    @TaskAction
    fun verify() {
        val modules = actualModules.get()
        val policies = policyModules.get()
        val missingPolicies = modules - policies
        if (missingPolicies.isNotEmpty()) {
            throw GradleException(
                "Missing module dependency policies: ${missingPolicies.sorted()}",
            )
        }

        val stalePolicies = policies - modules
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
