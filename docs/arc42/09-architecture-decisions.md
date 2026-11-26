# 09 Architecture decisions

Full records live in [`../adr/`](../adr/README.md). Legacy records are in [`../adr/legacy/`](../adr/legacy/README.md).

| ADR | Decision | Status |
|---|---|---|
| [0001](../adr/0001-java-25-gradle-9-toolchains.md) | Java 25 LTS, Gradle 9 toolchains, version catalog | Accepted |
| [0002](../adr/0002-multi-module-build.md) | Multi-module build with one-way dependencies | Accepted |
| [0003](../adr/0003-strangler-migration-legacy-module.md) | Legacy app kept as a module until parity; then archived as a standalone build | Completed |
| [0004](../adr/0004-libgdx-rendering-engine.md) | libGDX on LWJGL3 for rendering | Accepted; supersedes legacy 0001 |
| [0005](../adr/0005-scene2d-ui-native-file-dialog.md) | Scene2D UI, native file dialog | Accepted; supersedes legacy 0006 |
| [0006](../adr/0006-renderer-neutral-scene-and-display-mode-spi.md) | Renderer-neutral scene; display-mode and colour-scheme SPI | Accepted; supersedes legacy 0002 |
| [0007](../adr/0007-instanced-rendering-incremental-recolouring.md) | Instanced rendering; recolour without rebuild | Accepted; supersedes legacy 0008 |
| [0008](../adr/0008-mmcif-via-ciftools-java.md) | mmCIF via ciftools-java | Accepted; supersedes legacy 0004 |
| [0009](../adr/0009-spatial-grid-covalent-bonding.md) | Spatial-grid bonding with RasMol rules | Accepted |
| [0010](../adr/0010-maven-central-dependencies-only.md) | Maven Central dependencies only | Accepted; supersedes legacy 0007 |
| [0011](../adr/0011-error-handling-and-logging.md) | Typed exceptions, `System.Logger` | Accepted |
| [0012](../adr/0012-testing-strategy.md) | Testing strategy | Accepted |
| [0013](../adr/0013-ci-and-native-packaging.md) | GitHub Actions CI; jpackage installers | Accepted |
| [0014](../adr/0014-camera-interaction-and-picking.md) | Orbit camera; CPU ray picking | Accepted |
| [0015](../adr/0015-typed-properties-configuration.md) | Typed configuration from properties | Accepted; refines legacy 0005 |
| [0016](../adr/0016-bfactor-confidence-colouring-and-legends.md) | B-factor in the model; B-factor and confidence schemes; scheme legends | Accepted |
| [0017](../adr/0017-molecular-surfaces.md) | Molecular surfaces as a background-built overlay; distance fields and surface nets | Accepted |
| [0018](../adr/0018-computed-secondary-structure.md) | Computed secondary structure: DSSP subset, file annotation by default | Accepted |
| [0019](../adr/0019-cartoon-representation.md) | Cartoon representation: swept superellipse sections, indexed meshes | Accepted |
| [0020](../adr/0020-density-maps.md) | Density maps: MRC/CCP4 reader, surface-nets contours and zone colouring | Accepted |
| legacy [0003](../adr/legacy/0003-rasmol-derived-chemistry.md) | RasMol-derived chemistry and colours | Accepted (still in force) |
| legacy [0005](../adr/legacy/0005-properties-based-configuration.md) | Properties-based configuration | Accepted (still in force) |
