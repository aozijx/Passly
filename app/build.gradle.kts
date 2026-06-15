import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

// Release 签名优先读取环境变量，其次读取本地未跟踪 keystore.properties
val keystoreProperties = Properties().apply {
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) {
        load(FileInputStream(keystoreFile))
    }
}

fun resolveSigningValue(envName: String, propertyName: String): String? {
    return System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }
}

// Android 配置
android {
    namespace = "com.aozijx.passly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aozijx.passly"
        minSdk = 31
        targetSdk = 36
        versionCode = 8
        versionName = "0.3.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = resolveSigningValue("SIGNING_STORE_FILE", "signing.store.file")
            val storePasswordValue =
                resolveSigningValue("SIGNING_STORE_PASSWORD", "signing.store.password")
            val keyAliasValue = resolveSigningValue("SIGNING_KEY_ALIAS", "signing.key.alias")
            val keyPasswordValue =
                resolveSigningValue("SIGNING_KEY_PASSWORD", "signing.key.password")

            val hasCompleteSigningConfig = listOf(
                storeFilePath,
                storePasswordValue,
                keyAliasValue,
                keyPasswordValue
            ).all { !it.isNullOrBlank() }

            if (hasCompleteSigningConfig) {
                storeFile = file(requireNotNull(storeFilePath))
                storePassword = requireNotNull(storePasswordValue)
                keyAlias = requireNotNull(keyAliasValue)
                keyPassword = requireNotNull(keyPasswordValue)

                // 显式启用签名方案 (v2 和 v3)
                enableV2Signing = true   // 启用 APK 签名方案 v2 (默认 true，显式写出)
                enableV3Signing = true    // 启用 APK 签名方案 v3
            } else {
                // 签名信息不完整时回退到 debug 签名，保证本地/CI 可构建 release 产物
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true          // 启用 R8 代码压缩
            isShrinkResources = true        // 启用资源压缩
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("boolean", "EXPORT_ROOM_SCHEMA", "true")
        }

        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "EXPORT_ROOM_SCHEMA", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    // 让 androidTest 能读到 schemas/ 目录下的版本 JSON（用于 MigrationTestHelper）
    sourceSets.getByName("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }
}

// Kotlin 配置
kotlin {
    jvmToolchain(21)
}

room {
    // 指定 schema 导出目录，$projectDir 指向 app 模块目录
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.exifinterface)

    // Lifecycle & Navigation
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Jetpack Compose & UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)

    // Material Design & Icons
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.palette)
    ksp(libs.androidx.room.compiler)

    // Security & Biometric
    implementation(libs.androidx.biometric)

    // SQLCipher & SQLite
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite)

    // Data Persistence & Widgets
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Barcode Scanning & QR Code
    implementation(libs.barcode.scanning)
    implementation(libs.zxing.core)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)

    // Markdown
    implementation(libs.markdown.renderer)

    // Security KDF
    implementation(libs.argon2kt)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}