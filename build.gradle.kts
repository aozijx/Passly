// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    id("passly.module-boundaries")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.protobuf) apply false
}

moduleBoundaries {
    module(
        ":app",
        ":core:android",
        ":core:common",
        ":core:security",
        ":core:telemetry",
        ":core:ui",
        ":data",
        ":domain",
        ":runtime:session",
    )
    module(":core:android", ":core:telemetry")
    module(":core:common")
    module(":core:security", ":core:common", ":core:telemetry", ":domain")
    module(":core:telemetry", ":core:common")
    module(":core:ui", ":domain")
    module(
        ":data",
        ":core:android",
        ":core:common",
        ":core:security",
        ":core:telemetry",
        ":domain",
        ":runtime:session",
    )
    module(":domain", ":core:common")
    module(":runtime:session", ":domain")
}
