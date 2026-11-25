# 0013 CI with GitHub Actions and native packaging with jpackage

Status: Accepted

## Context
There is no CI, only macOS was ever supported, and the app is distributed as source (legacy deployment view, risk R2). Users should get an installer that bundles a Java runtime.

## Decision
- **CI** (`.github/workflows/ci.yml`): on push and pull request, run `./gradlew build` on `ubuntu-latest`, `macos-latest` and `windows-latest` with JDK 25 (toolchains provision others). Also compile `legacy-app` on JDK 11 until cutover. A non-blocking job runs the build with the newest JDK release.
- **Packaging**: a `jpackage` Gradle task in `jmvs-app` runs the JDK 25 toolchain's `jpackage` on the `installDist` output, with a jlink runtime of only the modules the app needs (`java.base, java.compiler, java.desktop, java.logging, java.sql, jdk.unsupported`, found with `jdeps`). It builds `.dmg` on macOS, `.msi` on Windows (needs WiX, present on GitHub runners) and `.deb` on Linux; `-Pjmvs.packageType=app-image` builds an unpacked app. No third-party packaging plugin is needed. macOS launch options include `-XstartOnFirstThread` ([0004](0004-libgdx-rendering-engine.md)). The version comes from `jmvs.version` in `gradle.properties`, overridden from the tag in releases.
- **Release** (`.github/workflows/release.yml`): on version tags, package on each OS and attach installers to a GitHub release.
- Code signing and notarisation are out of scope for now.

## Consequences
- Every change is built and tested on all three platforms.
- Users can install without a JDK. The macOS `.dmg` is about 48 MB; installers bundle LWJGL natives for all platforms.
- Unsigned macOS apps show a Gatekeeper warning; document how to open them.
