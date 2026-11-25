# 0010 Dependencies from Maven Central only

Status: Accepted. Supersedes legacy ADR-0007.

## Context
The legacy build pulls Java3D, JOGL and GlueGen jars of unknown provenance from `app/libs` (macOS natives only), and includes vendored Sun-copyright sources (`Sphere`, `Primitive`, `GeomBuffer`) and unattributed shader code (legacy risks R2–R4, debt D12). Guava is declared but unused.

## Decision
- New modules resolve every dependency from **Maven Central** (plugins from the Gradle Plugin Portal) through the version catalog. No jars or third-party sources are committed.
- Enable Gradle dependency verification (`gradle/verification-metadata.xml`) with checksums.
- Drop Guava. Write sphere/cylinder meshes and shaders in-repo with clear authorship.
- The vendored jars, Sun sources and `phong.*` shaders remain only inside `legacy-app/` and are deleted at cutover ([0003](0003-strangler-migration-legacy-module.md)).

## Consequences
- Known provenance, checksums and vulnerability scanning for all shipped code.
- Natives for all desktop platforms come from libGDX/LWJGL artifacts.
- Updating a dependency means updating its checksum entry as well: `./gradlew --write-verification-metadata sha256 help`.
