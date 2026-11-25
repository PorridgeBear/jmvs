# 04 Solution strategy

| Goal | Approach | ADR |
|---|---|---|
| Current, supported Java | Java 25 LTS via toolchains; Gradle 9 | [0001](../adr/0001-java-25-gradle-9-toolchains.md) |
| Replaceable renderer, testable core | Modules with one-way dependencies; renderer-neutral scene data | [0002](../adr/0002-multi-module-build.md), [0006](../adr/0006-renderer-neutral-scene-and-display-mode-spi.md) |
| Low-risk migration | Legacy app kept as a module until parity | [0003](../adr/0003-strangler-migration-legacy-module.md) |
| Cross-platform 3D | libGDX on LWJGL3 with custom GLSL | [0004](../adr/0004-libgdx-rendering-engine.md) |
| One-window UI | Scene2D overlay; native file dialog | [0005](../adr/0005-scene2d-ui-native-file-dialog.md) |
| Performance at ≥ 10k atoms | Instanced spheres/cylinders; colour buffers separate from geometry; grid-based bonding | [0007](../adr/0007-instanced-rendering-incremental-recolouring.md), [0009](../adr/0009-spatial-grid-covalent-bonding.md) |
| Robust input | ciftools-java; gzip and BinaryCIF | [0008](../adr/0008-mmcif-via-ciftools-java.md) |
| Match RasMol results | Keep RasMol rules and tables; parity tests | legacy ADR-0003, [0009](../adr/0009-spatial-grid-covalent-bonding.md), [0012](../adr/0012-testing-strategy.md) |
| Supply-chain hygiene | Maven Central with checksum verification | [0010](../adr/0010-maven-central-dependencies-only.md) |
| Robustness | Typed exceptions, errors shown in UI | [0011](../adr/0011-error-handling-and-logging.md) |
| Easy installation | CI on three OSes; jpackage installers | [0013](../adr/0013-ci-and-native-packaging.md) |
| Configurable without code | Typed config from classpath properties | [0015](../adr/0015-typed-properties-configuration.md) |

Layering: `jmvs-app` (UI, orchestration) → `jmvs-render-gdx` (GPU, input) → `jmvs-scene` (what to draw) → `jmvs-core` (what the molecule is). Each layer depends only on the one below through plain Java types.
