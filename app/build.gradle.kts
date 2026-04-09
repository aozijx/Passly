import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

// 读取 local.properties（放在文件顶部或根项目）
val localProperties = Properties().apply {
    val localPropFile = rootProject.file("local.properties")
    if (localPropFile.exists()) {
        load(FileInputStream(localPropFile))
    }
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
        flavorDimensions += listOf("scope")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 定义 Activity 类名常量
        buildConfigField("String", "VAULT_ACTIVITY_CLASS", "\"com.aozijx.passly.MainActivity\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProperties.getProperty("signing.store.file")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = localProperties.getProperty("signing.store.password")
                keyAlias = localProperties.getProperty("signing.key.alias")
                keyPassword = localProperties.getProperty("signing.key.password")
            } else {
                // 如果没有签名配置（如在 CI/CodeQL 环境中），回退到 debug 签名以保证编译通过
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true          // 启用 R8 代码压缩
            isShrinkResources = true        // 启用资源压缩
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

    productFlavors {
        // 功能范围 (完整 vs 仅保险箱)
        create("full") {
            dimension = "scope"
            buildConfigField("boolean", "IS_VAULT", "false")
        }
        create("vault") {
            dimension = "scope"
            applicationIdSuffix = ".vault" // 甚至可以作为独立包名共存
            versionNameSuffix = "-vault"
            // 覆盖默认值
            buildConfigField("boolean", "IS_VAULT", "true")
        }
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
    implementation(libs.androidx.security.crypto)

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
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}