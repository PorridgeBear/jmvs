# AGENTS.md

Guidance for AI coding agents and contributors working on JMVS, a RasMol-like 3D molecular viewer in Java. The project is being modernised from a legacy Java3D app to a libGDX app; see [`docs/modernisation.md`](docs/modernisation.md) for the roadmap and status, and [`docs/README.md`](docs/README.md) for the architecture docs.

## Repository map

| Path | Contents |
|---|---|
| `jmvs-core/` | Structure model, element table, covalent bonding, mmCIF readers (ciftools-java) |
| `jmvs-scene/` | Renderer-neutral scene data, display modes, colour schemes, registry, `jmvs.properties` |
| `jmvs-render-gdx/` | libGDX renderer, shaders, orbit camera, screen picking |
| `jmvs-app/` | Launcher, controller, Scene2D/VisUI UI, packaging (`jpackage` task) |
| `legacy-app/` | Legacy Java3D application, frozen; deleted at cutover |
| `build-logic/` | `jmvs.java-conventions` plugin (Java 25 toolchain, JUnit, `-Werror`) |
| `gradle/libs.versions.toml`, `gradle/verification-metadata.xml` | Dependency versions and checksums |
| `data/` | Sample mmCIF structures (`1crn`, `1usc`, `1zpd`, `6l1f`); used by tests |
| `docs/arc42/`, `docs/adr/` | To-be architecture and decisions |
| `docs/arc42/legacy/`, `docs/adr/legacy/` | As-is documentation of the legacy app |

## Build, run, test

Any JDK 17–27 can run Gradle; toolchains download JDK 25 (new modules) and JDK 11 (`legacy-app`).

```sh
./gradlew build                                    # compile and test everything
./gradlew :jmvs-core:test                          # one module's tests
./gradlew :jmvs-app:run --args="data/1crn.cif"     # run the new app (optionally with a file)
./gradlew :legacy-app:run                          # run the legacy app (macOS only)
./gradlew :jmvs-app:jpackage                       # native installer in jmvs-app/build/jpackage/
./gradlew --write-verification-metadata sha256 help   # after changing dependencies
```

Do not use plain `./gradlew run`: it starts both apps.

Visual and performance checks (render once to a PNG, or rotate and log the frame rate):

```sh
./gradlew :jmvs-app:installDist
jmvs-app/build/install/jmvs/bin/jmvs data/1usc.cif --mode ribbons --scheme structure --screenshot /tmp/1usc.png
jmvs-app/build/install/jmvs/bin/jmvs data/1zpd.cif --mode balls --benchmark 5
```

Mode ids: `balls`, `sticks`, `ballsandsticks`, `ribbons`. Scheme ids: `cpk`, `chain`, `group`, `structure`.

## Architecture rules

- Module dependencies point one way: `jmvs-app → jmvs-render-gdx → jmvs-scene → jmvs-core`.
- `jmvs-core` and `jmvs-scene` must not depend on libGDX, LWJGL, AWT or any GPU API.
- Display modes and colour schemes implement the renderer-neutral SPI in `jmvs-scene` and are registered in `Registry` and `jmvs.properties` ([ADR 0006](docs/adr/0006-renderer-neutral-scene-and-display-mode-spi.md)).
- Geometry never carries colour; recolouring must not rebuild geometry ([ADR 0007](docs/adr/0007-instanced-rendering-incremental-recolouring.md)).
- Shaders use GLSL ES 1.0 style (`attribute`, `varying`, `gl_FragColor`); `GlslCompatibility` turns them into GLSL 1.50.
- Do not change `legacy-app/` except to keep it building until cutover.

## Code conventions

- Java 25 language level: records, sealed types, pattern matching, `var` where it helps, immutable collections.
- No `System.exit`, `System.out` or `printStackTrace` outside the launcher; use `System.Logger` and typed exceptions ([ADR 0011](docs/adr/0011-error-handling-and-logging.md)).
- Load resources from the classpath, never via relative file paths.
- No static mutable state or singletons; pass dependencies in.
- Keep RasMol-derived chemistry rules intact (legacy ADR-0003) unless an ADR changes them.

## Testing

- Tests in `jmvs-core`, `jmvs-scene` and `jmvs-app` (controller) must run without a GPU or display.
- Use files in `data/` as golden inputs (tests read the `jmvs.dataDir` system property); assert counts, chains, secondary structure and bonds.
- Compilation uses `-Xlint:all -Werror`: fix warnings rather than suppressing them.
- Add or update tests with every behaviour change.

## Dependencies

- Maven Central only, declared in `gradle/libs.versions.toml`. No jars committed to the repository ([ADR 0010](docs/adr/0010-maven-central-dependencies-only.md)).

## Documentation

- Legacy docs are a record of the as-is system: do not edit them, except to add a *Superseded by* line to a legacy ADR.
- Record new or changed decisions as a new ADR in `docs/adr/` (next free number) and add it to `docs/adr/README.md`; supersede rather than rewrite accepted ADRs.
- Keep `docs/arc42/` and the status table in `docs/modernisation.md` in sync with the code.
