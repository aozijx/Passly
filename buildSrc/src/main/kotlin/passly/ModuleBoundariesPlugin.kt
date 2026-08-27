package passly

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

class ModuleBoundariesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "passly.module-boundaries must be applied to the root project"
        }

        val extension = target.extensions.create(
            "moduleBoundaries",
            ModuleBoundariesExtension::class.java,
        )
        val verification = target.tasks.register(
            "verifyModuleBoundaries",
            VerifyModuleBoundariesTask::class.java,
        ) {
            group = "verification"
            description = "Verifies the allowed direct Gradle project dependency graph."
            policyModules.set(extension.policyModules)
            allowedEdges.set(extension.allowedEdges)
            requiredEdges.set(extension.requiredEdges)
            sourceFiles.from(
                target.fileTree(target.rootDir) {
                    include("app/src/**/*.kt")
                    include("core/src/**/*.kt")
                    include("core/common/src/**/*.kt")
                    include("data/src/**/*.kt")
                    include("domain/src/**/*.kt")
                    include("runtime/session/src/**/*.kt")
                },
            )
            sourceRoot.set(target.layout.projectDirectory)
        }

        target.subprojects {
            tasks.matching { it.name == "check" }.configureEach {
                dependsOn(verification)
            }
        }

        target.gradle.projectsEvaluated {
            val boundaryProjects = target.subprojects.filter { project ->
                project.layout.projectDirectory.file("build.gradle.kts").asFile.isFile ||
                    project.layout.projectDirectory.file("build.gradle").asFile.isFile
            }
            verification.configure {
                actualModules.set(boundaryProjects.map(Project::getPath))
                actualEdges.set(
                    boundaryProjects.flatMap { sourceProject ->
                        sourceProject.configurations.flatMap { configuration ->
                            configuration.dependencies
                                .withType(ProjectDependency::class.java)
                                .mapNotNull { dependency ->
                                    dependency.path
                                        .takeUnless(sourceProject.path::equals)
                                        ?.let { targetPath ->
                                            edge(sourceProject.path, targetPath)
                                        }
                                }
                        }
                    }.distinct(),
                )
            }
        }
    }
}
