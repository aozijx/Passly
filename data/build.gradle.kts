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
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":runtime:session"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.sqlite)
    implementation(libs.sqlcipher)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.javalite)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.uuid.creator)
    implementation(libs.coil.core)

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
