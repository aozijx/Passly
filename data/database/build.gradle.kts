plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.aozijx.passly.data.database"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            buildConfigField("boolean", "EXPORT_ROOM_SCHEMA", "true")
        }
        getByName("debug") {
            buildConfigField("boolean", "EXPORT_ROOM_SCHEMA", "false")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets.getByName("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:common"))
    implementation(project(":core:telemetry"))
    api(project(":domain"))
    implementation(project(":runtime:session"))

    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    api(libs.androidx.room.paging)
    api(libs.androidx.sqlite)
    implementation(libs.sqlcipher)
    api(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
}
