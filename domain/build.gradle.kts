plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core:common"))
    testImplementation(libs.junit)
}
