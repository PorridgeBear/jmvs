# 0002 Plugin architecture via properties and reflection

Status: Accepted; partially realised

Superseded by [ADR 0006](../0006-renderer-neutral-scene-and-display-mode-spi.md).

## Context
README (2003): the code was "refactored ... around a plugin-oriented idea, more configurable, though never took this idea to its full extent, i.e. 3rd parties being able to drop in plugin jars".

## Decision
Display modes implement `JMVSPlugin`. `jmvs.properties` lists them (`plugin.list=balls,ribbons,sticks`) with `plugin.<id>.class=<FQCN>`. `AppearanceManager.initialisePlugins` instantiates via `Class.forName(...).newInstance()` and calls `init(Properties)`. The menu is built from each plugin's `getName()` and `getPaintModes()`; `JMVSPanel` identifies selections by menu label.

## Consequences
- Adding a built-in mode is a class plus a properties line.
- No discovery of external jars, no versioning/compat checks; plugins must be on the classpath.
- The SPI exposes Java3D `TransformGroup`, so plugins cannot be renderer-neutral.
- Menu labels double as identifiers; names must be unique.
- `paint()` in the SPI is unused (paint mode is pushed via `setPaintMode` before `render`).
- `plugin.default.*` is ignored; defaults hard-coded ("Balls"/"CPK").
- Plugin load failures are swallowed with a warning.
- Modernisation: consider `java.util.ServiceLoader` and a renderer-neutral return type.

Rationale: stated in README.
