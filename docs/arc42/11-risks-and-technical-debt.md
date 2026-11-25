# 11 Risks and technical debt

## 11.1 Java3D / JOGL dependency inventory (modernisation scope)

| Area | Files | Java3D API used |
|---|---|---|
| Scene/universe | `render/RenderScene` | `SimpleUniverse`, `Canvas3D`, `BranchGroup`, `TransformGroup`, `Switch`, `BoundingSphere`, lights, `View` clip/visibility |
| Interaction | `interaction/Rotator`, `Zoomer`, `Picker` | `MouseRotate`, `MouseZoom`, `PickMouseBehavior` |
| Plugin SPI | `JMVSPlugin`, `AppearanceManager` | `TransformGroup` is the return type of `render` — leaks the renderer into the contract |
| Balls | `JMVSPluginBalls` + `Sphere`, `Primitive`, `GeomBuffer` | `Material`, `Appearance`, `Transform3D`; vendored Sun utility code |
| Sticks / Balls&Sticks | `JMVSPluginSticks`, `JMVSPluginBallsAndSticks` | Java3D geometry/appearance **[Assumption: not read in detail]** |
| Ribbons | `JMVSPluginRibbons` | `ShaderAppearance`, `GLSLShaderProgram`, `SourceCodeShader`, `com.sun.j3d.utils.shader.StringIO` import (unused) |
| GUI | `JMVSPanel` | `Canvas3D` embedded in AWT `Panel` |
| Vecmath | plugins, `RenderScene` | `javax.vecmath.*` (`Vector3d`, `Point3d`, `Color3f`) |

Only `model`, `io`, `App` and `JMVSFileLoader` are free of Java3D (not of AWT).

## 11.2 Risks

| # | Risk | Impact | Mitigation idea |
|---|---|---|---|
| R1 | Java3D 1.x is unmaintained; unversioned binary jars tie the project to JDK ≤ 11 (per README) | Blocks JDK upgrades, security patches | Introduce a renderer abstraction, then migrate (e.g. JOGL/LWJGL/JavaFX 3D/jMonkeyEngine) — decision needed |
| R2 | Only macOS natives vendored | No Windows/Linux | Use published natives post-migration |
| R3 | Binary jars in git; provenance unclear (README: "some JARs that somebody had compiled") | Supply-chain and licensing | Replace with published artifacts |
| R4 | Sun-copyright vendored sources | Licence compliance **[Assumption]** | Legal review or replace |
| R5 | Performance ceiling (~1000 atoms, per README) | Poor UX on real proteins | Instancing, batching, spatial-grid bonding |
| R6 | `System.exit(0)` in library code | Silent app termination; untestable | Typed exceptions, errors in UI |
| R7 | Relative file paths for shaders | Breaks outside `app/` or when packaged | Load via classpath |
| R8 | Minimal tests; `AppTest` is a stub | Regression risk when refactoring | Golden-file model tests; render snapshot tests |

## 11.3 Technical debt

| # | Debt | Location |
|---|---|---|
| D1 | O(n²) bond detection with `pow`/`sqrt` and a `println` per bond | `Model.createCovalentBonds` |
| D2 | `findResidue(String.valueOf(i))` scans all residues per sequence id for each helix/sheet residue and matches on `label_seq_id` across **all** chains/models (correctness issue for multi-chain files) | `Molecule.addHelix/addSheet` |
| D3 | `getOrCreateModel` uses `models.get(number-1)` — fails if models start >1 or arrive out of order | `Molecule` |
| D4 | `computeBounds` initialises min/max at 0 and uses `else if`, so bounds are wrong for all-positive/negative coordinates; `computeMid` then adds onto the already-set `midX/Y/Z` before dividing | `Molecule.finish` |
| D5 | `display(..., minX - maxX, ...)` passes a negative extent into `centerView` (`Math.max` of negatives) | `JMVSPanel`, `RenderScene` — works only by accident **[Assumption]** |
| D6 | Parser: `split("\\s+")` breaks quoted values; category key taken via `keySet().toArray()[0]` on a `HashMap`; prints every row; no validation (`TODO`); default charset; reader never closed | `CIFReader` |
| D7 | Dead/unfinished: `isTurnPart` never set; `Picker` disabled; "ballsandsticks" disabled in config; `plugin.default.*` ignored; `JMVSPlugin.paint` unused | various |
| D8 | Raw types, deprecated `Class.newInstance()`, pre-Java-8 iterators, public mutable fields (`Molecule.minX…`) | various |
| D9 | `plugin.balls.divisions` parsed from properties inside the per-atom loop | `JMVSPluginBalls` |
| D10 | Duplicate colour logic across plugins; random chain colours unstable between runs | plugins, `AppearanceManager` |
| D11 | Static mutable singletons (`ElementsManager`, colour map) | model, appearance |
| D12 | Unused Guava **[Assumption]**; `phong.*` shaders unused (`setShaderProgram` commented out) and the vertex shader header credits a different author — provenance/licence to check | build, resources |
| D13 | Duplicated `1crn.cif`; a `.gz` sample the loader cannot read | repo |
| D14 | Deprecated `Dialog.show()`; hard-coded `new CIFReader()` bypasses the `MoleculeReader` abstraction | `JMVSFileLoader` |
