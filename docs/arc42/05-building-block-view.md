# 05 Building block view

## Level 1 — modules

```mermaid
flowchart TB
    app[jmvs-app<br/>launcher, Scene2D UI, controller, file handling] --> render[jmvs-render-gdx<br/>renderer, shaders, camera, picking]
    app --> scene
    app --> core
    render --> scene[jmvs-scene<br/>scene data, DisplayMode, ColourScheme, registry]
    scene --> core[jmvs-core<br/>model, elements, bonding, readers]
    core --> cif[(ciftools-java)]
    render --> gdx[(libGDX / LWJGL3)]
    app --> gdx
    legacy[legacy-app<br/>archived, standalone build]
```

| Module | Responsibility | Key packages |
|---|---|---|
| `jmvs-core` | Immutable structure model; element table; covalent bonding; readers for mmCIF/BinaryCIF (plain or gzip); resource loading and configuration errors | `core.model`, `core.chem`, `core.io`, `core.config` |
| `jmvs-scene` | Scene data types; display modes (Balls, Sticks, Balls-and-sticks, Ribbons); colour schemes (CPK, Chain, Group, Structure); registry; viewer config; ray picking | `scene`, `scene.mode`, `scene.colour`, `scene.config` |
| `jmvs-render-gdx` | Upload scene data to GPU, instanced drawing, ribbon meshes, lighting, GLSL setup, orbit camera, screen-to-ray picking | `render` |
| `jmvs-app` | Launcher and options; `ViewerController` state; Scene2D menus, status bar, dialogs; async loading; file dialog; drag-and-drop; packaging | `app`, `app.ui` |
| `legacy-app` | Legacy Java3D app, archived as a standalone build (not in the root build) | `com.adcworks.jmvs` (legacy) |

## Level 2 — `jmvs-core`

```mermaid
classDiagram
    Structure "1" o-- "*" Model
    Model "1" o-- "*" Chain
    Model "1" o-- "*" Bond
    Chain "1" o-- "*" Residue
    Residue "1" o-- "*" Atom
    Residue --> SecondaryStructure
    Atom --> Element
    StructureReader <|.. CifStructureReader
    StructureReaders --> StructureReader
    StructureBuilder ..> CovalentBonder
    StructureBuilder ..> Structure
```

- `Model`, `Chain`, `Residue`, `Atom`, `Bond` are records; `Structure` holds them as flat immutable lists and children refer to parents by index, so renderers can map atom indices to array slots. `StructureBuilder` groups atoms by model number, chain id (`label_asym_id`) and residue key, assigns secondary structure per chain, then bonds. Bounds and bounding sphere are computed once.
- `SecondaryStructure` is an enum: `HELIX`, `SHEET`, `TURN`, `COIL`.
- `ElementTable` is loaded from `elements.properties` and passed in (no singleton).
- `CifStructureReader` adapts ciftools-java; `StructureReaders` selects a reader by file extension and decompresses gzip.
- `CovalentBonder` applies RasMol rules over a hashed uniform grid.

## Level 2 — `jmvs-scene`

| Type | Responsibility |
|---|---|
| `Scene` | `SphereInstances`, `CylinderInstances`, `TriangleMesh` list, pick targets, bounding sphere; each element refers back to an atom index |
| `DisplayMode` | `Scene build(Structure)`; `supports(ColourScheme)`. Implementations: `BallsMode`, `SticksMode`, `BallsAndSticksMode`, `RibbonsMode` |
| `RibbonsMode`, `BSpline` | Carson–Bugg ribbons from Cα/O guide points with the legacy widths and helix offset; B-spline sampling |
| `ColourScheme` | `int colourOf(Structure, Atom)` (packed RGB); `ColourSchemes` builds CPK, Chain, Group, Structure from `ColourTables` |
| `Registry` | Ordered built-in modes and schemes by id; defaults from `ViewerConfig`; scheme fallback per mode |
| `Picking` | Nearest sphere hit by a ray |

## Level 2 — `jmvs-render-gdx`

| Type | Responsibility |
|---|---|
| `StructureRenderer` | Owns GPU buffers for the current scene; `setScene`, `setColours`, `setHighlight`, `render(camera)`; shaders from the classpath |
| `Primitives` | Unit icosphere and cylinder meshes used as instance templates |
| `GlslCompatibility` | Makes GLSL ES 1.0-style shaders compile on an OpenGL 3.2 core profile |
| `OrbitCameraController` | Rotate/zoom/pan/reset, framing from the bounding sphere, click detection |
| `ScreenPicker` | Ray from a screen point, delegating to `Picking` |

## Level 2 — `jmvs-app`

| Type | Responsibility |
|---|---|
| `Launcher`, `LaunchOptions` | Parses options, restarts on macOS without `-XstartOnFirstThread`, builds `Lwjgl3ApplicationConfiguration` (GL 3.2 core, MSAA, drag-and-drop) |
| `JmvsApplication` | libGDX `ApplicationListener`; wires stage, renderer, camera, controller, shortcuts; screenshot and benchmark modes |
| `ViewerController` | Current structure, mode, scheme, selection; background loading; per-mode scene cache; notifies a listener (no libGDX dependency) |
| `ViewerUi` | VisUI menu bar (File, Display, Colour, View, Help), status bar, error and About dialogs |
| `FileOpener` | Native open dialog via tinyfd |
