plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.aozijx.passly.data"
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
    api(project(":core:android"))
    api(project(":core:common"))
    api(project(":core:security"))
    api(project(":core:telemetry"))
    api(project(":domain"))
    implementation(project(":runtime:session"))

    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    api(libs.androidx.room.paging)
    api(libs.androidx.sqlite)
    implementation(libs.sqlcipher)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.javalite)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.uuid.creator)
    implementation(libs.argon2kt)
    implementation(libs.coil.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") {
                    option("lite")
                    outputSubDir = ""
                }
            }
        }
    }
}
