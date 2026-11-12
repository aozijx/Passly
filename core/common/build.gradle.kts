plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

val generateErrorCatalogSources =
    rootProject.tasks.named<passly.GenerateErrorCatalogTask>(
        "generateErrorCatalogSources",
    )

kotlin {
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )

    sourceSets.main {
        kotlin.srcDir(generateErrorCatalogSources.map { it.commonOutputDirectory })
    }
}

tasks.named("compileKotlin") {
    dependsOn(generateErrorCatalogSources)
}

dependencies {
    implementation(libs.uuid.creator)
    testImplementation(libs.junit)
}
