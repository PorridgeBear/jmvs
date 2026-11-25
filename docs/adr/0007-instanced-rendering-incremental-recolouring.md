# 0007 Instanced rendering and recolouring without geometry rebuilds

Status: Accepted. Supersedes legacy ADR-0008.

## Context
The legacy app creates a `TransformGroup` and tessellated `Sphere` per atom and rebuilds the whole scene graph on every mode or colour change (legacy ADR-0008). It slows down beyond ~1000 atoms (legacy risk R5); target structures have tens of thousands.

## Decision
- Draw all spheres with **one instanced draw call** of an icosphere (2 subdivisions, 320 triangles; 1 subdivision above 60,000 instances) and all cylinders with one instanced call of a 12-sided open cylinder; per-instance data is centre/ends, radius and colour.
- Draw ribbons as one triangle mesh per backbone segment with a per-vertex colour attribute, uploaded as non-indexed triangles because libGDX index buffers are 16-bit.
- Keep geometry buffers and colour buffers separate. Changing colour scheme rewrites only colour data; changing display mode rebuilds only that mode's geometry, and built geometry is cached per mode for the loaded structure.
- Selection highlighting is a per-instance flag (colour alpha), not new geometry.

## Consequences
- Draw calls are constant regardless of atom count. Measured on an Apple M1-class Mac at 2560×1504 with vsync off, `1zpd` (19,789 atoms): Balls 96 fps, Balls-and-sticks 108 fps, Sticks 131 fps, Ribbons 268 fps (`--benchmark`, arc42 §10).
- Requires OpenGL 3.2+ (`GL30`), available on all target platforms.
- Custom shaders are needed and must be maintained in-repo.
- Very large structures (> 1M atoms) may need level-of-detail or impostors; out of scope for now.
