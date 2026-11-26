# 0017 Molecular surfaces: grid distance fields, surface nets and a background overlay

Status: Accepted. Extends [0006](0006-renderer-neutral-scene-and-display-mode-spi.md) and
[0007](0007-instanced-rendering-incremental-recolouring.md); reuses the spatial grid of
[0009](0009-spatial-grid-covalent-bonding.md).

## Context
Surfaces are one of the most common views of a protein and were missing
([roadmap slice 11](../roadmap.md#11-molecular-surfaces)). The target: `4hhb` shows a smooth surface in under a few
seconds, coloured by chain, with transparency. A surface is usually drawn *with* another representation (over a
cartoon or ligand), but per-selection representations wait for slice 9. Surface computation is far slower than the
other display modes, so it cannot run on the render thread.

## Decision
- **An overlay, not a display mode.** A `Surface` (package `scene.surface`) is drawn over whatever `DisplayMode` is
  current, chosen from a *Surface* menu (None, Solvent-excluded, Solvent-accessible) with opacity presets, or with
  `--surface ses|sas` and `--surface-opacity`. Like `Scene` it is renderer-neutral and carries no colour: each vertex
  names the atom whose colour it takes, so every colour scheme applies and recolouring never rebuilds it.
- **Atoms**: the first model's heavy atoms, without waters, with RasMol united-atom radii (hydrogens implicit).
  Ligands such as haem are included.
- **Fields on a grid** (default spacing 0.5 Å, probe 1.4 Å, from `jmvs.properties`, [0015](0015-typed-properties-configuration.md)):
  - Solvent-accessible: signed distance `min(|p − c| − (r + probe))`, accumulated per atom within a band.
  - Solvent-excluded: an exact Euclidean distance transform with nearest-point labels (Felzenszwalb–Huttenlocher)
    of the grid points the probe centre can reach; the field is `probe − d`, with `d` reduced by the nearest
    reachable point's own distance outside the accessible surface to remove most of the grid-step error.
  - The grid is capped at `surface.maxVoxels` (8 million) by coarsening the spacing, which bounds memory for very
    large structures.
- **Meshing with naive surface nets** rather than marching cubes: one vertex per crossed grid cube at the mean of its
  edge crossings, one quad per crossed edge. It needs no case tables, gives about half the triangles and better
  shapes, and on our fields produces closed, consistently wound meshes (tested). Normals are the interpolated field
  gradient. Vertices are coloured by the atom whose van der Waals surface is nearest, found with the core
  `SpatialGrid` (extracted from the covalent bonder).
- **16-bit indexed meshes**: a surface is split into meshes of at most 65,535 vertices (shared vertices duplicated
  at splits), so the renderer can use libGDX's 16-bit index buffers instead of expanding to flat triangles.
- **Background build** on the loader thread. `ViewerController` caches one surface per kind per structure; a
  generation counter, also polled by the builder, cancels superseded builds (another kind, None, or a new file). The
  chosen kind persists across files. `--screenshot` and `--benchmark` wait for the surface.
- **Transparency without sorting**: an opaque surface draws normally; a transparent one first writes depth only,
  then blends with `GL_LEQUAL`, so only its nearest layer shows over the opaque scene. The fragment shader takes the
  alpha from a `u_opacity` uniform; blending keeps the framebuffer's alpha.

## Alternatives considered
- **Marching cubes**: the classic choice, but needs the 256-case tables and gives more, thinner triangles.
- **Analytic SES** (MSMS, reduced surface): exact, but much more complex and fragile on large structures.
- **Gaussian surfaces**: fast and smooth, but not the SES/SAS definitions users expect to compare with other viewers.
- **Surface as a `DisplayMode`**: fits the SPI as-is, but can't be combined with ribbons or ligands until slice 9.

## Consequences
- `4hhb` SES (278k triangles) builds in about 0.25 s; `1aon` (58,870 atoms, 1.2M triangles) in about 1–2 s. Drawn
  transparent over ribbons, `1aon` rotates at 107 fps (arc42 §10).
- Grid surfaces round the sharp creases of the accessible surface: crambin's SAS area is 4.4% below Shrake–Rupley at
  0.5 Å and 2.3% at 0.25 Å.
- Peak memory for a capped grid is roughly 150 MB of temporary arrays during the build.
- Surfaces aren't pickable; picking still uses the display mode's targets.
- Per-selection surfaces, hydrophobicity and electrostatic colouring are left to later slices.
