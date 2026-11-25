# 0006 Renderer-neutral scene model with display-mode and colour-scheme SPI

Status: Accepted. Supersedes legacy ADR-0002.

## Context
Legacy plugins return Java3D `TransformGroup`s, mix geometry with colouring, duplicate colour logic, are loaded reflectively, and use menu labels as identifiers (legacy ADR-0002, debts D7, D9, D10). Third-party plugin jars were an idea that was never realised and is not required now.

## Decision
In `jmvs-scene`:
- A **scene description** (`Scene`) made of plain data: sphere instances (centre, radius, atom index), cylinder instances (two ends, radius, atom index — a bond is two half-cylinders), triangle meshes (positions, normals, per-vertex atom index), pick targets (spheres used for picking) and a bounding sphere. No colours are baked into geometry.
- `DisplayMode` (`id`, `displayName`, `Scene build(Structure)`, `supports(ColourScheme)`) producing geometry, and `ColourScheme` (`id`, `displayName`, `int colourOf(Structure, Atom)` as packed RGB) producing one colour per atom. Any scheme can be combined with any mode; a mode may declare schemes it does not support (Ribbons does not support CPK), in which case the first supported scheme is used and the user's preference is restored on switching back.
- Built-in modes (Balls, Sticks, Balls-and-sticks, Ribbons) and schemes (CPK, Chain, Group, Structure) are registered in a `Registry` built in code; `jmvs.properties` selects the enabled entries, their order and defaults by **id**.
- Chain colours come from a fixed palette indexed by chain order, so they are stable between runs.
- No reflection and no loading of external jars.

## Alternatives considered
- **`ServiceLoader` drop-in jars**: useful for third parties; not needed and adds compatibility obligations. Can be added on top of the same interfaces later.

## Consequences
- Modes and schemes are unit-testable without a GPU.
- Recolouring needs only a new colour array ([0007](0007-instanced-rendering-incremental-recolouring.md)).
- Adding a mode or scheme means a class plus a registry entry; ids, not labels, identify them.
- The scene types become a stable internal contract between `jmvs-scene` and renderers.
