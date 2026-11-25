# 09 Architecture decisions

Full records live in [`../adr/`](../adr/README.md). All are retrospective; rationale is marked **[Inferred]** where not stated by the author.

| ADR | Decision | Status |
|---|---|---|
| [0001](../adr/0001-use-java3d-scene-graph.md) | Use Java3D scene graph for rendering | Accepted (legacy) — modernisation target |
| [0002](../adr/0002-plugin-architecture-via-properties.md) | Plugin SPI configured via properties + reflection | Accepted (partially realised) |
| [0003](../adr/0003-rasmol-derived-chemistry.md) | Port RasMol bonding, radii and colours | Accepted |
| [0004](../adr/0004-custom-mmcif-parser.md) | Hand-written mmCIF reader | Accepted (fragile) |
| [0005](../adr/0005-properties-based-configuration.md) | `.properties` config on classpath | Accepted |
| [0006](../adr/0006-awt-frame-ui.md) | AWT frame/menu UI hosting Canvas3D | Accepted (legacy) |
| [0007](../adr/0007-vendored-jars.md) | Vendor Java3D/JOGL jars in `app/libs` | Accepted (risky) |
| [0008](../adr/0008-rebuild-geometry-on-every-change.md) | Rebuild whole scene graph on each mode/paint change | Accepted (performance limiter) |
