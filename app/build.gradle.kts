import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.FilterConfiguration
import java.io.FileInputStream
import java.util.Locale
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
    namespace = "com.example.poop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.poop"
        minSdk = 31
        targetSdk = 36
        versionCode = 5
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            isMinifyEnabled = true
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

    // 定义
    flavorDimensions += listOf("scope")

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
            // 如果你想彻底在 Vault 中剔除某些代码，可以使用以下配置
             manifestPlaceholders["launcherActivity"] = ".MainActivity"
        }
    }
}

// 使用现代化的 androidComponents API 来重构 APK 重命名逻辑
androidComponents {
    onVariants { variant ->
        // 获取所有输出（处理 ABI 分包情况）
        variant.outputs.forEach { output ->
            val abi = output.filters.find {
                it.filterType == FilterConfiguration.FilterType.ABI
            }?.identifier ?: "universal"

            val verName = variant.outputs.map { it.versionName.getOrElse("1.0") }.first()

            // 构建任务名称
            val variantName = variant.name.replaceFirstChar { it.uppercase(Locale.getDefault()) }
            val abiSuffix =
                abi.replace("-", "").replaceFirstChar { it.uppercase(Locale.getDefault()) }
            val taskName = "copyAndRename${variantName}${abiSuffix}Apk"

            // 注册 Copy 任务
            val renameTask = tasks.register<Copy>(taskName) {
                // 显式设置重复文件处理策略为“覆盖”
                duplicatesStrategy = DuplicatesStrategy.INCLUDE

                from(variant.artifacts.get(SingleArtifact.APK))
                into(layout.buildDirectory.dir("outputs/renamed-apk/${variant.name.lowercase()}"))

                // 建议：精细化匹配源文件，避免通配符一次抓取到多个文件导致冲突
                include("**/$abi/*.apk", "**/*-$abi-*.apk")

                rename { fileName ->
                    if (fileName.endsWith(".apk")) "poop_v${verName}_${abi}.apk" else fileName
                }
            }

            // 【关键修复】使用更安全的监听方式来挂载依赖
            // 而不是直接用 tasks.named("assembleDebug")
            afterEvaluate {
                val assembleTaskName = "assemble$variantName"
                tasks.findByName(assembleTaskName)?.finalizedBy(renameTask)
            }
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

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
