# 0001 Java 25 LTS with Gradle 9 toolchains and a version catalog

Status: Accepted

## Context
The legacy build has no pinned toolchain, uses Gradle 7.5.1 and is capped at JDK 11 by vendored Java3D jars (legacy ADR-0007, risk R1). Developers may have any JDK installed; the maintainer's machine currently has OpenJDK 27 (non-LTS). Dependency versions are scattered in `app/build.gradle.kts`.

## Decision
- Target **Java 25 LTS** for all new modules, compiled through a Gradle Java toolchain (`languageVersion = 25`), so the installed JDK is irrelevant to the bytecode level.
- Use the **Gradle 9** wrapper (9.9.x at time of writing) with the foojay toolchain resolver plugin so missing JDKs (25, and 11 for `legacy-app`) are provisioned automatically.
- Gradle itself runs on whatever JDK 17–27 is installed (Gradle 9.9 supports running on JDK 27). If a future local JDK is newer than Gradle supports, pin the daemon JVM with `gradle/gradle-daemon-jvm.properties` (`./gradlew updateDaemonJvm --jvm-version=25`).
- `-Pjmvs.javaVersion=<n>` overrides the toolchain version, for trying newer JDKs in CI without changing the baseline.
- Declare all dependency and plugin versions in `gradle/libs.versions.toml`; shared build logic lives in the `jmvs.java-conventions` plugin under `build-logic/`.
- Enable the configuration cache, build cache and parallel execution (`gradle.properties`).

## Alternatives considered
- **Java 21 LTS**: older, fewer language features; no dependency requires it.
- **Track the latest JDK (27)**: non-LTS, shorter support window for packaged runtimes.

## Consequences
- Modern language features (records, sealed types, pattern matching) are available everywhere.
- Builds are reproducible across machines and CI regardless of the local JDK.
- First build downloads JDKs (Temurin 25 and 11); offline builds need them pre-installed.
- Kotlin DSL compilation of `build-logic` prints a notice when Gradle runs on a JDK newer than Kotlin supports; harmless.
- CI runs an extra non-blocking job on the newest JDK to catch upcoming breakage ([0013](0013-ci-and-native-packaging.md)).
