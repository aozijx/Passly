import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

// 读取 local.properties
// 显式指定类型以消除 "Platform Type" 警告
val localProperties: Properties = Properties()
val localPropertiesFile: File = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.example.poop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.poop"
        minSdk = 31
        targetSdk = 36
        versionCode = 5
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 配置支持的 CPU 架构
        ndk {
            // 限制打包进 APK 的本地库架构
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    signingConfigs {
        create("release") {
            // 这里使用 Safe Call 和 Explicit Type 处理从 Properties 读取的平台类型字符串
            storeFile = localProperties.getProperty("signing.store.file")?.let { file(it) }
            storePassword = localProperties.getProperty("signing.store.password")
            keyAlias = localProperties.getProperty("signing.key.alias")
            keyPassword = localProperties.getProperty("signing.key.password")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            // 获取 SigningConfig 时也显式处理
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning?.storeFile != null) {
                signingConfig = releaseSigning
            }
            buildConfigField("boolean", "EXPORT_ROOM_SCHEMA", "true")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "EXPORT_ROOM_SCHEMA", "false")
        }
    }
    // 1. 启用 APK 拆分配置
    splits {
        abi {
            isEnable = true // 开启拆分
            reset() // 重置默认包含的所有架构
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64") // 指定要拆分的架构
            isUniversalApk = true // 是否额外生成一个包含所有架构的通用 APK
        }
    }

    // 2. 自定义生成的 APK 文件名
    applicationVariants.all {
        val variant = this
        outputs.forEach { output ->
            val apkOutput = output as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            // 获取当前 APK 对应的架构名，如果是 universal APK 则返回 null
            val abi = apkOutput.getFilter("ABI") ?: "universal"
            // 设置新的文件名：应用名_版本号_架构.apk
            apkOutput.outputFileName = "poop_v${variant.versionName}_${abi}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    // 指定 schema 导出目录，$projectDir 指向 app 模块目录
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

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

    // Utils
    implementation(libs.guava)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
