plugins {
    id("jmvs.java-conventions")
    application
}

description = "JMVS desktop application"

dependencies {
    implementation(project(":jmvs-render-gdx"))
    implementation(platform(libs.lwjgl.bom))
    implementation(libs.gdx.backend.lwjgl3)
    implementation(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    implementation(libs.visui)
    implementation(libs.lwjgl.tinyfd)
    listOf("natives-linux", "natives-linux-arm64", "natives-macos", "natives-macos-arm64", "natives-windows", "natives-windows-arm64")
        .forEach { runtimeOnly(variantOf(libs.lwjgl.tinyfd) { classifier(it) }) }
}

val osName = System.getProperty("os.name").lowercase()
val isMac = "mac" in osName
val isWindows = "windows" in osName

// libGDX/LWJGL load native libraries and use sun.misc.Unsafe.
val runtimeJvmArgs = listOf("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
// GLFW must own the first thread on macOS (docs/adr/0004-libgdx-rendering-engine.md). Not added to the
// portable start scripts, where the launcher relaunches the JVM with it instead.
val macJvmArgs = listOf("-XstartOnFirstThread")

application {
    mainClass = "com.adcworks.jmvs.app.Launcher"
    applicationName = "jmvs"
    applicationDefaultJvmArgs = runtimeJvmArgs
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    if (isMac) {
        jvmArgs(macJvmArgs)
    }
}

// Native installer with a trimmed Java runtime (docs/adr/0013-ci-and-native-packaging.md).
// Type defaults to dmg / msi / deb for the build OS; override with -Pjmvs.packageType=app-image (or pkg, exe, rpm).
val jpackageExecutable = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
    .map { it.metadata.installationPath.file("bin/" + if (isWindows) "jpackage.exe" else "jpackage").asFile.path }
val appVersion = providers.gradleProperty("jmvs.version")
val packageType = providers.gradleProperty("jmvs.packageType")
    .orElse(if (isMac) "dmg" else if (isWindows) "msi" else "deb")

tasks.register<Exec>("jpackage") {
    group = "distribution"
    description = "Builds a native installer for the current OS with a bundled Java runtime."
    dependsOn(tasks.named("installDist"))
    val input = layout.buildDirectory.dir("install/jmvs/lib")
    val output = layout.buildDirectory.dir("jpackage")
    inputs.dir(input)
    outputs.dir(output)
    val javaOptions = runtimeJvmArgs + if (isMac) macJvmArgs else emptyList()
    val type = packageType
    val version = appVersion
    val tool = jpackageExecutable
    inputs.property("type", type)
    inputs.property("version", version)
    val osOptions = when {
        isMac -> listOf("--mac-package-identifier", "com.adcworks.jmvs")
        isWindows -> listOf("--win-menu", "--win-shortcut", "--win-dir-chooser")
        else -> listOf("--linux-shortcut", "--linux-package-name", "jmvs")
    }
    argumentProviders.add {
        listOf(
            "--type", type.get(),
            "--name", "JMVS",
            "--app-version", version.get(),
            "--vendor", "adc works",
            "--description", "RasMol-like 3D molecular viewer",
            "--input", input.get().asFile.path,
            "--main-jar", "jmvs-app.jar",
            "--main-class", "com.adcworks.jmvs.app.Launcher",
            "--add-modules", "java.base,java.compiler,java.desktop,java.logging,java.sql,jdk.unsupported",
            "--jlink-options", "--strip-debug --no-header-files --no-man-pages --strip-native-commands",
            "--dest", output.get().asFile.path,
        ) + javaOptions.flatMap { listOf("--java-options", it) } + osOptions
    }
    doFirst {
        output.get().asFile.deleteRecursively()
        executable = tool.get()
    }
}
