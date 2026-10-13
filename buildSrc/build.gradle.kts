plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal {
        content {
            excludeGroup("org.jetbrains.kotlin")
        }
    }
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
