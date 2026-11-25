# JMVS modernisation plan

Living roadmap for moving JMVS from the legacy Java3D application to a modern, cross-platform viewer. Detailed rationale lives in the [to-be ADRs](adr/README.md) and [to-be arc42](arc42/); the as-is system is described in [`arc42/legacy/`](arc42/legacy/) and [`adr/legacy/`](adr/legacy/README.md). This document is the index and status tracker — update the status table as phases complete.

## Status

| Phase | Scope | Status |
|---|---|---|
| 0 | Documentation: this plan, `AGENTS.md`, to-be ADRs and arc42 | Done |
| 1 | Build foundation: Gradle 9 multi-module, toolchains, legacy module, CI | Done (CI not yet run on GitHub) |
| 2 | Core: domain model, mmCIF reading, bonding, tests | Done |
| 3 | Scene: renderer-neutral scene model and display modes | Done |
| 4 | Renderer: libGDX rendering, camera, picking | Done |
| 5 | Application: launcher, UI, async loading, quality validation | Done on macOS; Windows/Linux validation open |
| 6 | Packaging and cutover: installers, legacy removal | Packaging done (macOS `.dmg` verified); cutover open |

## Problem

JMVS (~4k LOC) is a 1999/2003 Java3D viewer revived in 2022. The legacy [risks and technical debt](arc42/legacy/11-risks-and-technical-debt.md) show it is pinned to JDK ≤ 11 by unmaintained Java3D 1.x and vendored macOS-only JOGL jars, carries vendored Sun-licensed sources, slows down beyond ~1000 atoms, terminates via `System.exit`, depends on the working directory, and is almost untested.

## Decisions

| Topic | Decision | ADR |
|---|---|---|
| Java | Java 25 LTS via Gradle toolchains; Gradle 9 wrapper; version catalog | [0001](adr/0001-java-25-gradle-9-toolchains.md) |
| Build structure | Multi-module Gradle build with enforced dependency direction | [0002](adr/0002-multi-module-build.md) |
| Migration | Strangler: legacy app kept as `legacy-app` on a JDK 11 toolchain until parity | [0003](adr/0003-strangler-migration-legacy-module.md) |
| Rendering | libGDX (LWJGL3 backend) replaces Java3D/JOGL | [0004](adr/0004-libgdx-rendering-engine.md) |
| UI | libGDX Scene2D UI + native file dialog | [0005](adr/0005-scene2d-ui-native-file-dialog.md) |
| Extensibility | Renderer-neutral scene model; display-mode and colour-scheme SPI with built-in registry; no external jar loading | [0006](adr/0006-renderer-neutral-scene-and-display-mode-spi.md) |
| Performance | Instanced rendering; recolour without geometry rebuild | [0007](adr/0007-instanced-rendering-incremental-recolouring.md) |
| mmCIF | rcsb `ciftools-java`, gzip support | [0008](adr/0008-mmcif-via-ciftools-java.md) |
| Bonding | Spatial-grid search with RasMol rules | [0009](adr/0009-spatial-grid-covalent-bonding.md) |
| Dependencies | Maven Central only; remove vendored jars and Sun sources | [0010](adr/0010-maven-central-dependencies-only.md) |
| Errors/logging | Typed exceptions, `System.Logger`, no `System.exit` in library code | [0011](adr/0011-error-handling-and-logging.md) |
| Testing | Golden files, RasMol parity, GPU-free scene tests | [0012](adr/0012-testing-strategy.md) |
| CI/packaging | GitHub Actions matrix; jpackage installers per OS | [0013](adr/0013-ci-and-native-packaging.md) |
| Interaction | Orbit camera; CPU ray picking | [0014](adr/0014-camera-interaction-and-picking.md) |
| Configuration | Classpath `.properties`, parsed once into typed config | [0015](adr/0015-typed-properties-configuration.md) |

## Target module layout

```
settings.gradle.kts
gradle/libs.versions.toml
jmvs-core/        model, chemistry (elements, RasMol bonding), io (ciftools adapter, gzip); no UI/GL deps
jmvs-scene/       renderer-neutral scene data, DisplayMode/ColourScheme SPI, built-in modes and schemes
jmvs-render-gdx/  libGDX renderer: instanced spheres/cylinders, ribbon meshes, lighting, camera, picking
jmvs-app/         LWJGL3 launcher, Scene2D UI, file dialog, async loading, packaging
legacy-app/       current app/ moved as-is on a JDK 11 toolchain; deleted at cutover
```

Dependencies point one way: `jmvs-app → jmvs-render-gdx → jmvs-scene → jmvs-core`. Only `jmvs-render-gdx` and `jmvs-app` may depend on libGDX. See [05 Building block view](arc42/05-building-block-view.md).

## Risk and debt resolution

References are to the legacy [risks and technical debt](arc42/legacy/11-risks-and-technical-debt.md).

| Item | Resolution | Phase |
|---|---|---|
| R1 Java3D / JDK ≤ 11 lock | libGDX renderer; Java 25 | 1, 4 |
| R2 macOS-only natives | LWJGL natives from Maven Central; CI on macOS, Linux, Windows | 1, 4 |
| R3 Binary jars in git | Maven Central only; `app/libs` removed with `legacy-app` | 6 |
| R4 Sun-copyright sources | Not ported; deleted with `legacy-app` | 6 |
| R5 Performance ceiling | Spatial-grid bonding, instancing, no per-atom logging, incremental recolouring | 2, 4 |
| R6 `System.exit` in library code | Typed exceptions surfaced in the UI | 2, 5 |
| R7 Relative shader paths | All resources loaded from the classpath | 4 |
| R8 Minimal tests | Golden-file, parity and scene tests in CI | 1–3 |
| D1 O(n²) bonding with logging | Spatial grid, no per-bond output | 2 |
| D2 Secondary structure matched by seq id across all chains | Lookup keyed by (model, chain, seq id) | 2 |
| D3 Model lookup by list index | Models keyed by model number | 2 |
| D4 Wrong bounds/centroid | Correct min/max initialisation and centroid | 2 |
| D5 Negative extent passed to centring | Camera framed from the bounding sphere | 4 |
| D6 Fragile parser | `ciftools-java` | 2 |
| D7 Dead/unfinished features | Turns parsed and coloured; balls-and-sticks enabled; defaults honoured; picking implemented; unused `paint()` dropped | 2–5 |
| D8 Pre-Java-8 style | Records, sealed types, pattern matching, immutable collections | 2–5 |
| D9 Config parsed per atom | Typed config parsed once | 3 |
| D10 Duplicate colour logic, unstable chain colours | Shared `ColourScheme` implementations; deterministic palette | 3 |
| D11 Static mutable singletons | Injected `ElementTable`, no static state | 2 |
| D12 Unused Guava, unused/unattributed shaders | Removed; new shaders written in-repo | 1, 4, 6 |
| D13 Duplicated/unreadable samples | One copy per file; `.gz` readable | 2, 6 |
| D14 Deprecated dialog API, hard-coded reader | Native dialog; reader selected by file extension | 2, 5 |

## Phases

### Phase 0 — Documentation

- This plan, root [`AGENTS.md`](../AGENTS.md), [to-be ADRs](adr/README.md), [to-be arc42](arc42/), *Superseded by* pointers on the affected legacy ADRs, updated [`docs/README.md`](README.md).
- Exit: docs reviewed and merged.

### Phase 1 — Build foundation

- Gradle 9 wrapper, `gradle/libs.versions.toml`, foojay toolchain resolver.
- Move `app/` to `legacy-app/` on a JDK 11 toolchain; it must still build and run on macOS.
- Create empty `jmvs-core`, `jmvs-scene`, `jmvs-render-gdx`, `jmvs-app` modules on JDK 25.
- Make `./gradlew` work when the local JDK is newer than 25 (e.g. 27): check Gradle's supported runtime and, if needed, pin the daemon JVM with `gradle/gradle-daemon-jvm.properties`.
- GitHub Actions: build and test on Ubuntu, macOS, Windows; non-blocking job on the latest JDK.
- Exit: `./gradlew build` passes locally and in CI; `./gradlew :legacy-app:run` still works on macOS.
- Outcome: current state tagged `legacy-java3d`; Gradle 9.9.2 runs directly on JDK 27, so no daemon pin was needed; dependency checksums in `gradle/verification-metadata.xml`.

### Phase 2 — Core (`jmvs-core`)

- Immutable domain model, `ElementTable` from `elements.properties`, model number and chain-aware residue lookup, correct bounds, turns.
- `MoleculeReader` SPI with a `ciftools-java` implementation (`_atom_site`, `_struct_conf`, `_struct_sheet_range`), `.cif.gz` support, typed exceptions.
- Spatial-grid covalent bonding with RasMol rules.
- Golden-file tests over `data/*.cif`, bond parity against the legacy implementation, load-time test for `1zpd`.
- Exit: all sample files load; golden and parity tests pass in CI.
- Outcome: 1crn matches the legacy test (1 model, 1 chain, 46 residues, 327 atoms); bonds equal the all-pairs RasMol rules on every sample; `1zpd` reads and bonds in ~0.1–0.2 s. None of the samples contains turns; a synthetic file covers them.

### Phase 3 — Scene (`jmvs-scene`)

- Scene data types (sphere/cylinder instances, triangle meshes, RGBA colours), `DisplayMode` and `ColourScheme` SPI, registry configured from properties, deterministic chain palette.
- Balls, Sticks, Balls-and-sticks and Ribbons (Carson–Bugg splines) emit scene data; Structure colouring includes turns.
- Exit: unit tests (no GPU) cover each mode and scheme.

### Phase 4 — Renderer (`jmvs-render-gdx`)

- Instanced sphere/cylinder shaders, ribbon meshes, lighting; recolouring updates colour buffers only.
- Orbit/zoom/pan camera framed from the bounding sphere; CPU ray picking with highlight.
- Exit: all modes and schemes render `1usc` correctly on macOS.
- Outcome: LWJGL aligned to 3.3.6 (Java 25 JNI support); GLSL 1.50 via libGDX prepend code; picking uses a linear scan ([ADR 0014](adr/0014-camera-interaction-and-picking.md)).

### Phase 5 — Application (`jmvs-app`)

- LWJGL3 launcher with macOS first-thread handling, Scene2D menus (Data, Display, Colour) and status bar, native open dialog, drag-and-drop, async loading with progress, error dialogs.
- Validate [quality scenarios](arc42/10-quality-requirements.md) on macOS and one other OS.
- Exit: feature parity with legacy F1–F8; quality targets met.
- Outcome: F1–F9 implemented; quality results in [arc42 §10](arc42/10-quality-requirements.md) (1zpd Balls 96 fps). `--screenshot` and `--benchmark` options support manual checks. Open: run on Windows and Linux.

### Phase 6 — Packaging and cutover

- jpackage installers per OS built in CI on tags.
- Parity check against the legacy screenshots, delete `legacy-app/`, dedupe sample data, refresh the README comparison screenshots in `docs/images/` (legacy ones are in `docs/images/legacy/`), update root `README.md`, `AGENTS.md`, arc42 risks, and mark [ADR 0003](adr/0003-strangler-migration-legacy-module.md) complete.
- Exit: release tag with installers for macOS, Windows and Linux.
- Outcome so far: `./gradlew :jmvs-app:jpackage` builds a 48 MB `.dmg` that starts and renders; `release.yml` packages on all three OSes on `v*` tags. Open: first tagged release, Windows/Linux installer checks, parity sign-off and legacy removal.

## Considerations

- Developers may have a JDK newer than 25 installed (e.g. 27). Code is compiled with the toolchain (Java 25); the local JDK only runs Gradle, and Gradle 9.9 supports running on JDK 27.
- libGDX on macOS needs GLFW on the first thread: handled with `-XstartOnFirstThread` and a launcher restart ([ADR 0004](adr/0004-libgdx-rendering-engine.md)).
- The libGDX LWJGL3 backend targets desktop OpenGL; macOS caps at OpenGL 4.1, which is enough for instancing.
- `ciftools-java` 9.0.0 works on Java 25.
- `./gradlew run` starts both the legacy and the new app; use `:jmvs-app:run` or `:legacy-app:run`.
- Legacy docs stay as written except for *Superseded by* lines; to-be docs refer to them as "legacy ADR-000n".
- Revisit the to-be docs at cutover so they describe what was actually built.
