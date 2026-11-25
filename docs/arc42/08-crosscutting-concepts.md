# 08 Cross-cutting concepts

| Concept | Approach | ADR |
|---|---|---|
| Configuration | `jmvs.properties` and `elements.properties` on the classpath, parsed once into immutable records, validated at startup | [0015](../adr/0015-typed-properties-configuration.md) |
| Colouring | `ColourScheme` implementations shared by all display modes; colours stored separately from geometry; fixed chain palette | [0006](../adr/0006-renderer-neutral-scene-and-display-mode-spi.md), [0007](../adr/0007-instanced-rendering-incremental-recolouring.md) |
| Units | Model and scene coordinates in ångströms (Å); element radii converted from RasMol units (÷ 250) when the table is loaded; no ad-hoc scale factors | |
| Immutability | Structure model and config are immutable; scene data is built once per (structure, mode) | |
| Threading | Render thread owns GL and UI; loading, bonding and scene building run on a background executor and post results with `Gdx.app.postRunnable` | [0005](../adr/0005-scene2d-ui-native-file-dialog.md) |
| Error handling | Typed exceptions; app boundary shows a dialog and keeps running | [0011](../adr/0011-error-handling-and-logging.md) |
| Logging | `System.Logger`; summaries at INFO, per-item detail only at TRACE | [0011](../adr/0011-error-handling-and-logging.md) |
| Resources | Shaders and properties loaded from the classpath next to the class that uses them; VisUI skin from its jar | |
| Shaders | GLSL ES 1.0 style, compiled as GLSL 1.50 via libGDX prepend code; one shared two-sided Blinn-Phong fragment shader | [0004](../adr/0004-libgdx-rendering-engine.md) |
| Dependency injection | Constructor injection, wired in the launcher; no static state or reflection | [0006](../adr/0006-renderer-neutral-scene-and-display-mode-spi.md) |
| Testing | GPU-free tests for core and scene; golden files from `data/` | [0012](../adr/0012-testing-strategy.md) |
| Security | Reads only user-chosen local files; no network; dependency checksums verified | [0010](../adr/0010-maven-central-dependencies-only.md) |
| Code style | Java 25 idioms; see [`AGENTS.md`](../../AGENTS.md) | [0001](../adr/0001-java-25-gradle-9-toolchains.md) |
