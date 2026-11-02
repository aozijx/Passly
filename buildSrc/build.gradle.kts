plugins {
    `kotlin-dsl`
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.8.0")
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}

repositories {
    mavenCentral()
    gradlePluginPortal {
        content {
            excludeGroup("org.jetbrains.kotlin")
        }
    }
}

dependencies {
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("moduleBoundaries") {
            id = "passly.module-boundaries"
            implementationClass = "passly.ModuleBoundariesPlugin"
        }
    }
}
