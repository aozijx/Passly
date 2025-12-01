plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.github.ben-manes.versions") version "0.53.0"
}

android {
    namespace = "com.example.poop"
    //noinspection OldTargetApi
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.poop"
        minSdk = 31
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["WECHAT_PACKAGE"] = "com.tencent.mm"
        manifestPlaceholders["QQ_PACKAGE"] = "com.tencent.mobileqq"
        manifestPlaceholders["DOUYIN_PACKAGE"] = "com.ss.android.ugc.aweme.lite"

        buildConfigField("String", "WECHAT_PACKAGE", "\"com.tencent.mm\"")
        buildConfigField("String", "QQ_PACKAGE", "\"com.tencent.mobileqq\"")
        buildConfigField("String", "DOUYIN_PACKAGE", "\"com.ss.android.ugc.aweme.lite\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false // 发布版不混淆，方便用户查看代码
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // 设置 Java 11 兼容性
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11" // 设置 Kotlin JVM 目标为 11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.coil.compose)  // 添加 Coil
    implementation(libs.coil.gif)  // GIF 支持
    implementation(libs.coil.svg)  // SVG 支持
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// 强制将 androidx.lifecycle 相关模块使用 2.8.3（降级传递的 2.9.0）
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "androidx.lifecycle") {
            useVersion("2.8.3")
            because("Require lifecycle 2.8.x to stay compatible with compileSdk 34")
        }
    }
}
