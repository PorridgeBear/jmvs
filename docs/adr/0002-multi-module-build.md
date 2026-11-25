# 0002 Multi-module build with one-way dependencies

Status: Accepted

## Context
The legacy app is a single module in which Java3D types leak into the plugin SPI, interaction and GUI (legacy arc42 §11.1). Only `model` and `io` are renderer-free, and nothing enforces that.

## Decision
Split the build into Gradle modules:

| Module | Responsibility | May depend on |
|---|---|---|
| `jmvs-core` | Domain model, element table, bonding, structure readers | `ciftools-java` |
| `jmvs-scene` | Renderer-neutral scene data, display-mode and colour-scheme SPI, built-in modes | `jmvs-core` |
| `jmvs-render-gdx` | libGDX renderer, camera, picking | `jmvs-scene`, libGDX |
| `jmvs-app` | Launcher, UI, file handling, packaging | all of the above, libGDX |
| `legacy-app` | Legacy Java3D app until cutover ([0003](0003-strangler-migration-legacy-module.md)) | none of the above |

Dependencies point one way: `jmvs-app → jmvs-render-gdx → jmvs-scene → jmvs-core`. Each module uses `implementation` dependencies by default and `api` only for types in its public signatures.

## Alternatives considered
- **Single module with packages**: simpler build, but the renderer boundary cannot be enforced and tests of core code would pull in GL dependencies.
- **JPMS modules**: stronger encapsulation, but libGDX is not modularised; revisit later.

## Consequences
- The compiler enforces that core and scene code cannot use libGDX.
- Core and scene tests run without a GPU.
- A different renderer could be added as another `jmvs-render-*` module.
- More build files; shared configuration goes in convention plugins.
