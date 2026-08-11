plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core:common"))
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
