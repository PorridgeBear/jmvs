# 04 Solution strategy

| Goal | Approach | ADR |
|---|---|---|
| Render 3D without raw OpenGL | Java3D retained-mode scene graph | [0001](../../adr/legacy/0001-use-java3d-scene-graph.md) |
| Swappable display modes | `JMVSPlugin` interface, classes named in `jmvs.properties`, loaded reflectively | [0002](../../adr/legacy/0002-plugin-architecture-via-properties.md) |
| Match RasMol results | Port RasMol bonding rules, radii, CPK table | [0003](../../adr/legacy/0003-rasmol-derived-chemistry.md) |
| Read modern structure files | Hand-written line-based mmCIF reader dispatching by reflection | [0004](../../adr/legacy/0004-custom-mmcif-parser.md) |
| Config without code changes | Classpath `.properties` (plugins, colours, elements) | [0005](../../adr/legacy/0005-properties-based-configuration.md) |
| Desktop UI | AWT frame + menu bar hosting a `Canvas3D` | [0006](../../adr/legacy/0006-awt-frame-ui.md) |
| Run on Apple Silicon | Vendored third-party jars | [0007](../../adr/legacy/0007-vendored-jars.md) |
| Mode/paint switching | Rebuild the whole scene graph | [0008](../../adr/legacy/0008-rebuild-geometry-on-every-change.md) |

Layering: `gui` → `render` (+ `appearance` plugins) → `model`; `io` produces `model`. Rendering plugins consume the domain model directly and emit Java3D `TransformGroup`s — the domain model is Java3D-free, the plugins are not.
