// Shared configuration for all new JMVS modules: Java 25 toolchain and JUnit.
plugins {
    `java-library`
}

val libs = versionCatalogs.named("libs")

java {
    toolchain {
        // -Pjmvs.javaVersion lets CI try newer JDKs without changing the baseline.
        languageVersion = JavaLanguageVersion.of(providers.gradleProperty("jmvs.javaVersion").getOrElse("25"))
    }
}

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("assertj").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-Xlint:-processing", "-Werror"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    val dataDir = rootProject.layout.projectDirectory.dir("data")
    // Sample files are test inputs, so changing them re-runs tests.
    inputs.dir(dataDir).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("sampleData")
    systemProperty("jmvs.dataDir", dataDir.asFile.absolutePath)
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
