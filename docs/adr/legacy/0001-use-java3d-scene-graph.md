# 0001 Use Java3D scene graph for rendering

Status: Accepted (retrospective); **primary modernisation target**

Superseded by [ADR 0004](../0004-libgdx-rendering-engine.md).

## Context
The project (1998/99) set out to build a RasMol-like viewer in Java. README: "Java3D had just come about which abstracted low level OpenGL to a more abstract Scene Graph approach, though lower level primitives were possible."

## Decision
Render with Java3D 1.x: `SimpleUniverse` + `Canvas3D`, a scene graph of `BranchGroup`/`TransformGroup`/`Switch`, lights, `Material`/`Appearance`, mouse behaviours (`RenderScene`, plugins, `interaction`).

## Consequences
- Rapid development of rotation/zoom/lighting with little OpenGL code.
- Each atom is its own `TransformGroup` + `Sphere` node: poor scaling (README: ~1000 atoms).
- Java3D leaks into the plugin SPI (`render` returns `TransformGroup`), the interaction classes and the GUI; only `model`/`io` are independent.
- Java3D is unmaintained; ties build to old JDKs and vendored natives (see [0007](0007-vendored-jars.md)).
- Modernisation should first introduce a renderer-neutral scene description (atoms, bonds, ribbons, colours) in front of Java3D.

Rationale: stated in README (abstraction level); the lock-in consequences are **[Inferred]**.
