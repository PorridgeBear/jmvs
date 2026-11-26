# 0018 Computed secondary structure: a DSSP subset, applied when the file has none

Status: Accepted. Extends [0006](0006-renderer-neutral-scene-and-display-mode-spi.md); reuses the spatial grid of
[0009](0009-spatial-grid-covalent-bonding.md).

## Context
Ribbons and the Structure colour scheme read each residue's secondary structure, which came only from the file's
`_struct_conf` and `_struct_sheet_range`. Predicted models and some files have no such annotation, so they showed as
all coil ([roadmap slice 5](../roadmap.md#5-computed-secondary-structure)). Slice 6 (cartoon) also depends on good
helix and strand assignment.

## Decision
- **Assign from backbone hydrogen bonds in `jmvs-core`** (`chem.DsspAssigner`), following the core of DSSP
  (Kabsch & Sander, 1983):
  - Amide H placed 1.0 Å from N, opposite the previous residue's C=O (none after a chain break or for proline);
    file hydrogens are ignored, as in DSSP.
  - Electrostatic energy `0.084 × 332 × (1/rON + 1/rCH − 1/rOH − 1/rCN)`; an H-bond if below −0.5 kcal/mol,
    keeping the two best acceptors per donor. Candidate O atoms come from `SpatialGrid` within 5.2 Å of N; bonds
    may cross chains within a model.
  - n-turns (3, 4, 5), minimal helices (α first, then 3-10 and π only where free), parallel and antiparallel
    bridges, ladders and bulges (gaps ≤ 1 and ≤ 4).
  - Reduced to the existing four states: H, G, I → `HELIX`; E → `SHEET`; T → `TURN`; B, S and the rest → `COIL`.
  - Chain breaks are C(i)–N(i+1) > 2.5 Å; residues missing N, CA, C or O and non-polymer residues stay `COIL`.
- **Readers are unchanged.** `Structure` records whether the file had any annotation
  (`secondaryStructureAnnotated()`), and `withSecondaryStructure(list)` returns a copy that shares models, chains,
  atoms and bonds, so atom indices, scenes' atom references, selection and surfaces stay valid.
- **Source choice** (`SecondaryStructureSource`): *Auto* (default: the file's annotation if it has any, else
  computed), *From file*, or *Computed*. Chosen in the *Display* menu or with `--ss auto|file|computed`, and kept
  when another file is opened. `ViewerController` keeps the structure as read and the one shown; switching source
  applies it and rebuilds the current mode's scene on the background executor, then sends `sceneChanged`.
- **Tests compare with the samples' annotation by agreement, not exact match**: three-state per-residue agreement
  of 80–97% (thresholds a little below the measured values), plus golden computed counts. The AlphaFold sample's
  annotation is DSSP 4's own; `AF-P69905-F1-model_v6-noss.cif` has it removed to exercise *Auto*.

## Alternatives considered
- **Full DSSP 4** (PPII, bends, accessibility, eight-state output): more code for states the viewer can't show yet.
- **CA-only methods** (e.g. P-SEA): work for CA-only models, but agree less with the annotations users compare
  against; DSSP is the reference PDB and AlphaFold DB use.
- **Always compute**: consistent, but would change the look of every annotated file; authors' annotation is kept by
  default.

## Consequences
- Predicted models and unannotated files show helices and sheets in ribbons and Structure colouring.
- Assignment is fast (about 50 ms for the 59,000-atom `1aon`), so it runs at load without a progress indicator.
- Computed helix ends differ slightly from older author annotations (e.g. `4hhb`); users can pick *From file*.
- CA-only structures still get no secondary structure.
