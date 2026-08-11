plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.uuid.creator)
    testImplementation(libs.junit)
}
