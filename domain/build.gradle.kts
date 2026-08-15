plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    api(project(":core:common"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.uuid.creator)
    
    testImplementation(libs.junit)
}
