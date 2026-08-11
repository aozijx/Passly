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
        ":domain",
        ":feature:auth:api",
        ":feature:recovery:api",
        ":runtime:session",
    )
    module(":core:android")
    module(":core:common")
    module(":core:security", ":domain")
    module(":core:telemetry", ":core:common")
    module(":core:ui", ":domain")
    module(":domain", ":core:common")
    module(":feature:auth:api", ":domain")
    module(":feature:recovery:api", ":domain")
    module(":runtime:session", ":domain")
}
