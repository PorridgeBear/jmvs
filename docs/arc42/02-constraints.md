# 02 Constraints

## Technical

| Constraint | Detail | ADR |
|---|---|---|
| Java | Java 25 LTS through Gradle toolchains; any newer local JDK only runs Gradle | [0001](../adr/0001-java-25-gradle-9-toolchains.md) |
| Build | Gradle 9 wrapper, version catalog, multi-module | [0001](../adr/0001-java-25-gradle-9-toolchains.md), [0002](../adr/0002-multi-module-build.md) |
| Rendering | libGDX 1.14.x, LWJGL3 backend (LWJGL aligned to 3.3.6), OpenGL 3.2 core profile minimum (macOS max 4.1) | [0004](../adr/0004-libgdx-rendering-engine.md) |
| UI | libGDX Scene2D in the render window; native file dialog via LWJGL tinyfd | [0005](../adr/0005-scene2d-ui-native-file-dialog.md) |
| macOS threading | GLFW must run on the first thread (`-XstartOnFirstThread`; the launcher restarts the JVM if missing) | [0004](../adr/0004-libgdx-rendering-engine.md) |
| Dependencies | Maven Central only, checksum-verified; no committed jars | [0010](../adr/0010-maven-central-dependencies-only.md) |
| Input formats | PDBx/mmCIF (plain or gzip) and BinaryCIF | [0008](../adr/0008-mmcif-via-ciftools-java.md) |
| Layering | `jmvs-core` and `jmvs-scene` free of GL/UI dependencies | [0002](../adr/0002-multi-module-build.md) |
| Transitional | `legacy-app` on JDK 11 with vendored jars until cutover | [0003](../adr/0003-strangler-migration-legacy-module.md) |

## Organisational

| Constraint | Detail |
|---|---|
| Single maintainer hobby project | Prefer simple, well-supported tools; avoid heavy frameworks |
| Behavioural parity with RasMol | Bonding rules, radii and CPK colours are kept (legacy ADR-0003) |
| Licensing | Only dependencies with permissive licences (Apache 2.0, MIT, BSD); no third-party source copied in |
| Documentation | arc42 + ADRs; legacy docs are kept as a record ([`AGENTS.md`](../../AGENTS.md)) |

## Conventions

Package root `com.adcworks.jmvs`; Java 25 idioms; `System.Logger`; classpath resources; no static mutable state; spaces for indentation.
