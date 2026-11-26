// Standalone build for the archived legacy Java3D app (docs/adr/0003-strangler-migration-legacy-module.md).
// Not part of the root build; run with ./gradlew -p legacy-app run (macOS only).
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "legacy-app"
