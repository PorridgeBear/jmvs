# 0019 Cartoon representation: swept superellipse sections, indexed meshes

Status: Accepted. Extends [0006](0006-renderer-neutral-scene-and-display-mode-spi.md); refines the mesh upload of
[0007](0007-instanced-rendering-incremental-recolouring.md); builds on [0018](0018-computed-secondary-structure.md).

## Context
Cartoon is the representation most users expect: helices, arrowed strands and coil tubes, with nucleic acids as a
backbone and bases ([roadmap slice 6](../roadmap.md#6-cartoon-representation)). The Ribbons mode draws flat
Carson–Bugg strips without arrowheads or thickness (accepted debt A6), and nucleic acids drew nothing. Ribbons stays
unchanged for RasMol parity.

## Decision
- **A new `DisplayMode`, `CartoonMode` (`cartoon`)**, registered next to Ribbons. Shared code is extracted from
  `RibbonsMode` without changing its output: `BackboneGuides` (Cα/O segments split at Cα–Cα > 4.5 Å, peptide-plane
  frame) and `Vectors`.
- **Proteins**: an open B-spline (order 3) through the Cα atoms, and a second one through the per-residue
  Carson–Bugg width directions (flipped where the carbonyl oxygen changes side), give each sample a centre and frame.
  A cross-section ring of 16 vertices is swept along it:
  - Cross-sections are superellipses `|x/w|^e + |y/h|^e = 1` with analytic normals: e = 2 (ellipse) for helices
    and coil, e = 6 (rounded rectangle) for strands. One ring topology covers every shape, so sizes and exponents
    blend linearly over a residue at each change.
  - Helix 2.4 × 0.5 Å, strand 2.0 × 0.5 Å, coil tube radius 0.3 Å; the last residue of each strand is an arrowhead
    from 3.4 Å wide down to the coil radius, starting abruptly. Values are `cartoon.*` keys in `jmvs.properties`
    (`CartoonSettings`, like [0015](0015-typed-properties-configuration.md)).
  - Segment ends are closed with flat caps.
- **Nucleic acids** (polymer residues without CA but with C1' and P or C4'): a tube (radius 0.8 Å) through P (C4' at
  a 5' end), with a parallel-transported frame; per base, a stick (instanced cylinder) from that atom to C1' and a
  flat block from C1' to the Watson–Crick edge (N1 of purines, N3 of pyrimidines) in the base plane.
- **Colour and picking as for other modes**: every vertex names its residue's Cα (or P, or C1' for bases), so all
  residue-level schemes apply and recolouring never rebuilds geometry; CPK is not supported, as for Ribbons. Pick
  targets are Cα, P and C1'.
- **Indexed scene meshes**: meshes are split to at most 65,535 vertices (a mesh per segment, long segments in
  overlapping chunks), and the renderer uploads any scene mesh within that limit with 16-bit indices, as surfaces
  already were; larger meshes still fall back to non-indexed triangles. This replaces 0007's "uploaded as
  non-indexed triangles" for meshes that fit.

## Alternatives considered
- **Separate box and tube sweeps** joined at transitions: crisper strand edges, but seams and more code at every
  change; the superellipse looks close to a box at e = 6.
- **Cylinders for helices**: some users prefer them; kept for a later option.
- **Base blocks as full ring polygons**: more faithful, but a block conveys pairing and stacking at this scale.

## Consequences
- `4hhb`, `1usc` and `1bna` render as cartoons; frame rates stay high (`1zpd` 263 fps, `1aon` 184 fps on the
  reference Mac, against 304 and 267 for Ribbons).
- Indexed upload also roughly halves Ribbons' vertex memory and raised its frame rate (`1zpd` 268 → 304 fps).
- Smoothness depends on the secondary structure assignment: computed assignment (0018) gives unannotated files
  arrows and helices too.
- Arrowhead side faces use cross-section normals, ignoring their taper; not visible at normal sizes.
