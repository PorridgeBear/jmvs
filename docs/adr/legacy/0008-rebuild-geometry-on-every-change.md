# 0008 Rebuild geometry on every mode/paint change

Status: Accepted (performance limiter)

## Context
Plugins return a complete `TransformGroup` for a molecule (`JMVSPlugin.render`). Paint mode is set on the plugin before rendering; `paint(TransformGroup, String)` exists but is unused (call commented out in `AppearanceManager.render`).

## Decision
`JMVSPanel` calls `AppearanceManager.render` and `RenderScene.display` on every load and every menu selection; `display` stops the renderer, detaches the previous branch, adds and compiles the new one, restarts the renderer.

## Consequences
- Simple state model: scene = f(molecule, mode, paint).
- Colour changes pay full geometry cost (spheres with 20 divisions per atom, ribbon splines).
- Camera centring is recomputed on every change.
- The abandoned `paint()` hook suggests incremental recolouring was considered **[Inferred]**.
- Modernisation: separate geometry from appearance/colour so recolouring doesn't rebuild.
