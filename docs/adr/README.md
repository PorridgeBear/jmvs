# Architecture Decision Records (to-be)

Decisions for the modernised JMVS. The as-is records live in [`legacy/`](legacy/README.md) and are referred to as "legacy ADR-000n" because numbering restarts here. Roadmap and status: [`../modernisation.md`](../modernisation.md).

| ADR | Title | Status | Relation to legacy |
|---|---|---|---|
| [0001](0001-java-25-gradle-9-toolchains.md) | Java 25 LTS with Gradle 9 toolchains and a version catalog | Accepted | |
| [0002](0002-multi-module-build.md) | Multi-module build with one-way dependencies | Accepted | |
| [0003](0003-strangler-migration-legacy-module.md) | Strangler migration with the legacy app kept as a module | Completed | Legacy app archived as a standalone build |
| [0004](0004-libgdx-rendering-engine.md) | libGDX (LWJGL3 backend) as the rendering engine | Accepted | Supersedes legacy 0001 |
| [0005](0005-scene2d-ui-native-file-dialog.md) | Scene2D UI with a native file dialog | Accepted | Supersedes legacy 0006 |
| [0006](0006-renderer-neutral-scene-and-display-mode-spi.md) | Renderer-neutral scene model with display-mode and colour-scheme SPI | Accepted | Supersedes legacy 0002 |
| [0007](0007-instanced-rendering-incremental-recolouring.md) | Instanced rendering and recolouring without geometry rebuilds | Accepted | Supersedes legacy 0008 |
| [0008](0008-mmcif-via-ciftools-java.md) | Read mmCIF with rcsb ciftools-java | Accepted | Supersedes legacy 0004 |
| [0009](0009-spatial-grid-covalent-bonding.md) | Spatial-grid covalent bonding with RasMol rules | Accepted | Keeps legacy 0003 |
| [0010](0010-maven-central-dependencies-only.md) | Dependencies from Maven Central only | Accepted | Supersedes legacy 0007 |
| [0011](0011-error-handling-and-logging.md) | Error handling and logging | Accepted | |
| [0012](0012-testing-strategy.md) | Testing strategy | Accepted | |
| [0013](0013-ci-and-native-packaging.md) | CI with GitHub Actions and native packaging with jpackage | Accepted | |
| [0014](0014-camera-interaction-and-picking.md) | Camera interaction and CPU ray picking | Accepted | |
| [0015](0015-typed-properties-configuration.md) | Typed configuration from classpath properties | Accepted | Refines legacy 0005 |
| [0016](0016-bfactor-confidence-colouring-and-legends.md) | B-factor in the model, confidence colouring and colour-scheme legends | Accepted | |
| [0017](0017-molecular-surfaces.md) | Molecular surfaces: grid distance fields, surface nets and a background overlay | Accepted | |
| [0018](0018-computed-secondary-structure.md) | Computed secondary structure: a DSSP subset, applied when the file has none | Accepted | |
| [0019](0019-cartoon-representation.md) | Cartoon representation: swept superellipse sections, indexed meshes | Accepted | |

Legacy ADRs 0003 (RasMol-derived chemistry) and 0005 (properties configuration) stay in force.

## Writing a new ADR

- Use the next free number and the format of the existing records: title, `Status`, `Context`, `Decision`, optional `Alternatives considered`, `Consequences`.
- Do not rewrite an accepted ADR; add a new one that supersedes it and update the status line of the old one.
- Add it to this index and to [arc42 §9](../arc42/09-architecture-decisions.md).
