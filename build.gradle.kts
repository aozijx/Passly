// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    id("passly.module-boundaries")
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.protobuf) apply false
}

val ktlintEngineVersion = libs.versions.ktlint.get()

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(ktlintEngineVersion)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(ktlintEngineVersion)
    }

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn("ktlintCheck")
    }
}

val ktlintCheckBuildLogic =
    tasks.register<GradleBuild>("ktlintCheckBuildLogic") {
        dir = layout.projectDirectory.dir("buildSrc").asFile
        buildName = "passlyBuildLogicCheck"
        tasks = listOf("ktlintCheck")
    }

val ktlintFormatBuildLogic =
    tasks.register<GradleBuild>("ktlintFormatBuildLogic") {
        dir = layout.projectDirectory.dir("buildSrc").asFile
        buildName = "passlyBuildLogicFormat"
        tasks = listOf("ktlintFormat")
    }

tasks.register("ktlintCheckAll") {
    group = "verification"
    description = "Checks Kotlin source and Gradle Kotlin scripts in every project."
    dependsOn("ktlintCheck")
    dependsOn(subprojects.map { "${it.path}:ktlintCheck" })
    dependsOn(ktlintCheckBuildLogic)
}

tasks.register("ktlintFormatAll") {
    group = "formatting"
    description = "Formats Kotlin source and Gradle Kotlin scripts in every project."
    dependsOn("ktlintFormat")
    dependsOn(subprojects.map { "${it.path}:ktlintFormat" })
    dependsOn(ktlintFormatBuildLogic)
}

moduleBoundaries {
    module(
        ":app",
        ":core",
        ":core:common",
        ":data",
        ":domain",
        ":feature:autofill",
        ":runtime:session",
        ":security",
    )
    module(":core:common")
    module(":core", ":core:common", ":domain")
    module(
        ":data",
        ":core",
        ":core:common",
        ":domain",
        ":runtime:session",
    )
    module(":domain", ":core:common")
    module(":runtime:session", ":domain")
    module(":security", ":core", ":core:common", ":domain", ":runtime:session")
    module(":feature:autofill", ":core", ":domain", ":runtime:session")
    requireDirectDependency(":app", ":feature:autofill")
    requireDirectDependency(":app", ":domain")
    requireDirectDependency(":data", ":domain")
    requireDirectDependency(":security", ":domain")
}
