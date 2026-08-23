plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("moduleBoundaries") {
            id = "passly.module-boundaries"
            implementationClass = "passly.ModuleBoundariesPlugin"
        }
    }
}
