// Legacy Java3D application, archived as a standalone build (docs/adr/0003-strangler-migration-legacy-module.md).
plugins {
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.8.2")

    implementation("com.google.guava:guava:31.0.1-jre")

    implementation(fileTree("libs") { include("*.jar") })
}

application {
    mainClass = "com.adcworks.jmvs.App"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
