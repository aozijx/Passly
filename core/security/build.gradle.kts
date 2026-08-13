plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.aozijx.passly.core.security"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    api(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:telemetry"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.argon2kt)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
}
