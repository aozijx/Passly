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
        ":core",
        ":core:common",
        ":data",
        ":domain",
        ":runtime:session",
    )
    module(":core:common")
    module(":core", ":core:common", ":domain")
    module(
        ":data",
        ":core",
        ":core:common",
        ":domain",
        ":runtime:session",
    )
    module(":domain", ":core:common")
    module(":runtime:session", ":domain")
}
