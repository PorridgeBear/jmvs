# 0020 Density maps: MRC/CCP4 reader, surface-nets contours and zone colouring

Status: Accepted. Extends [0017](0017-molecular-surfaces.md) (shares its meshing and overlay rendering) and
[0007](0007-instanced-rendering-incremental-recolouring.md); reuses the spatial grid of
[0009](0009-spatial-grid-covalent-bonding.md).

## Context
Cryo-EM structures are deposited as a density map (EMDB) plus a model fitted into it (PDB). Checking a model against
its map is a core cryo-EM task and JMVS could not show maps at all ([roadmap slice 14](../roadmap.md#14-density-maps)).
The target: an EM map and its fitted model shown together, with a contour level the user can adjust interactively.
Slice 11 already meshes a scalar field on a grid with surface nets and draws an overlay, opaque or transparent.

## Decision
- **Reader** (`core.io.MrcMapReader`, `core.map.DensityMap`): MRC2014 and CCP4 maps (`.map`, `.mrc`, `.ccp4`,
  optionally gzipped), as served by EMDB. Byte order from the machine stamp, else guessed from the header; data modes
  0, 1, 2, 6 and 12; `MAPC/MAPR/MAPS` axis order; extended header skipped; origin from the MRC2014 `ORIGIN` field when
  set, else start indices × voxel size; oblique cells via the usual fractional-to-Cartesian matrix. Statistics (min,
  max, mean, rms) are computed rather than read from the header, which is often stale. Errors are a typed
  `MapReadException` ([0011](0011-error-handling-and-logging.md)).
- **Memory cap**: maps above `density.maxVoxels` (32 million, 128 MB of floats) are binned while reading by averaging
  b³ blocks, with the smallest b that fits; the origin moves to the first block's centre.
- **Contour by surface nets**, shared with surfaces: `SurfaceNets`, `Grid` and the 16-bit chunking move to a public
  `scene.mesh` package. `SurfaceNets.extract` takes a level and which side is inside, so a map is meshed in place in
  grid-index space without copying it per level; vertices and normals (by the inverse transpose) are then moved to
  Cartesian space. Chunks keep each quad's two triangles together, so the **mesh style** draws the quad outlines
  (`GL_LINES`, no diagonals) from the same vertices; the surface style draws the triangles. Switching style re-uploads
  indices only.
- **Default level** `mean + 2.5σ` (`density.sigma`), where σ is the map's rms. On the sample EMD-3488 this is 0.094,
  close to the depositors' recommended 0.09; levels are shown both absolute and in σ. `--contour` takes either.
- **Adjustable contour**: a slider (−1σ to the map maximum, capped at 15σ, in 0.05σ steps), `[` / `]` (0.1σ, Shift for
  0.5σ) and the *Map* menu. Each change starts a background build; a generation counter cancels superseded builds
  between slabs, and the previous contour stays shown until the new one is ready, so dragging stays smooth.
- **Zone colouring**: each contour vertex takes the colour of the nearest heavy atom of the model's first model within
  `density.zoneRadius` (3 Å), found with `SpatialGrid`; other vertices use `density.colour` (grey). The vertex atom
  `-1` (`Isosurface.NO_ATOM`) means "map colour", so every colour scheme applies and recolouring never rebuilds
  geometry. When another structure opens the contour's geometry is reused: it is shown in the map colour at once, then
  re-zoned in the background.
- **Independent of the structure**: a map can be shown alone, with any structure, and stays when another structure is
  opened. Files are routed by extension (File > Open, drag-and-drop, positional argument or `--map`).
- **Fetching maps by EMDB ID is left to [slice 2](../roadmap.md#2-open-by-id)**, so downloading and caching are
  designed once for PDB and EMDB IDs.

## Alternatives considered
- **Marching cubes** for maps: the classic choice, but surface nets is already tested and gives quads that make a
  clean mesh style.
- **Contour level as a fraction of enclosed volume** (ChimeraX's 1% default): on the sample map, whose box is mostly
  solvent, 1% gives 0.167, nearly twice the recommended level and missing much of the model; σ-based is the convention
  users know from Coot and PyMOL.
- **Uniform map colour**: simpler, but zone colouring shows at a glance which density the model explains and which
  chain it belongs to.
- **Reading the EMDB header XML for the recommended level**: needs network access or a second file; left to slice 2.

## Consequences
- The sample map (100³) contours in about 50 ms warm and 0.2–0.6 s cold, including zone colouring (arc42 §10).
- Peak memory is the map plus the mesh: contouring keeps only two slabs of cube indices; binning bounds the map.
- Maps aren't pickable, and only the first model's atoms are used for zones.
- Surfaces now split meshes by quads; their tests and golden areas are unchanged.
