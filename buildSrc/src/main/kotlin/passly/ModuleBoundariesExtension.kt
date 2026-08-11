package passly

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

abstract class ModuleBoundariesExtension @Inject constructor(
    objects: ObjectFactory,
) {
    internal val policyModules: SetProperty<String> =
        objects.setProperty(String::class.java)

    internal val allowedEdges: SetProperty<String> =
        objects.setProperty(String::class.java)

    fun module(source: String, vararg allowedTargets: String) {
        policyModules.add(source)
        allowedEdges.addAll(allowedTargets.map { target -> edge(source, target) })
    }
}

internal const val EDGE_SEPARATOR = " -> "

internal fun edge(source: String, target: String): String =
    "$source$EDGE_SEPARATOR$target"
