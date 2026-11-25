# 0007 Vendored Java3D/JOGL jars

Status: Accepted (risky)

Superseded by [ADR 0010](../0010-maven-central-dependencies-only.md).

## Context
README (2022): getting the sources to run on Apple M1 "was a headache but I managed to find some JARs that somebody had compiled for the M1"; "you'll need to add the appropriate JARs for gluegen and jogl for your architecture to app/libs". Java 11 is the latest supporting these jars.

## Decision
Commit `j3dcore`, `j3dutils`, `vecmath`, `jogl-all`, `gluegen-rt` and macOS universal natives to `app/libs`, consumed with `implementation(fileTree("libs") { include("*.jar") })`.

## Consequences
- Builds/runs on Apple Silicon without a repository for these artifacts.
- Unknown provenance/versions, no checksums, no vulnerability scanning.
- Only macOS natives; other platforms require manual jar swaps.
- Caps the JDK at 11 and blocks modular/modern Java.
- Modernisation: replace with published artifacts or a different renderer.
