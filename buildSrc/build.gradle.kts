plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("moduleBoundaries") {
            id = "passly.module-boundaries"
            implementationClass = "passly.ModuleBoundariesPlugin"
        }
    }
}
