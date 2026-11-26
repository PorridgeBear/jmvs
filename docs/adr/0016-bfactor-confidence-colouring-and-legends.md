# 0016 B-factor in the model, confidence colouring and colour-scheme legends

Status: Accepted. Extends [0006](0006-renderer-neutral-scene-and-display-mode-spi.md).

## Context
Predicted models (AlphaFold DB and others) are everyday inputs, and their per-residue confidence (pLDDT, 0–100) is
stored in `_atom_site.B_iso_or_equiv`. JMVS ignored that column, so neither B-factors nor confidence could be shown
([roadmap slice 1](../roadmap.md#1-alphafold-ready-colouring)). Colours that encode a value also need a key, which no
existing scheme had.

## Decision
- `Atom` carries `bFactor` from `B_iso_or_equiv`, or `NaN` when the file has none (`hasBFactor()`). The model has no
  separate pLDDT field: it is the same column, and its meaning depends on the file.
- Two schemes, enabled in `jmvs.properties` like the others:
  - **B-factor** (`bfactor`): low → mid → high gradient (blue, white, red), scaled to the lowest and highest
    B-factor of the loaded structure, as RasMol's temperature colouring.
  - **Confidence (pLDDT)** (`confidence`): the AlphaFold DB bands and colours (very high > 90, confident > 70,
    low > 50, very low), with the same exclusive boundaries as the AlphaFold DB viewer.
  Both are always offered, with no detection of predicted models; on an experimental structure Confidence simply
  bands the B-factors. Atoms without a B-factor get a neutral "missing" colour. All colours are typed in
  `ColourTables` and required in the configuration ([0015](0015-typed-properties-configuration.md)).
- `ColourScheme` gains `Optional<Legend> legend(Structure)`, empty by default. `Legend` is a renderer-neutral sealed
  type in `jmvs-scene`: `Bands` (labelled colours) or `Gradient` (colour stops with the minimum and maximum value).
  The UI draws it bottom-left over the view; it changes with the scheme and structure without touching geometry
  ([0007](0007-instanced-rendering-incremental-recolouring.md)).
- The status bar shows the picked atom's value: `pLDDT` while colouring by confidence, `B-factor` otherwise.

## Alternatives considered
- **Detect predicted models** (ModelCIF `_ma_qa_metric` categories) and hide or auto-select Confidence. Rejected for
  now: simpler to always offer both, and files from other predictors don't all carry those categories.
- **Fixed B-factor range** from configuration. Rejected: useful ranges vary widely between structures and
  resolutions; the structure's own range always uses the full gradient.

## Consequences
- New value-based schemes (e.g. hydrophobicity) can provide a legend without UI changes.
- The B-factor gradient is relative, so colours are not comparable between structures; the legend shows the range.
- `Atom` grew a component; readers added later (e.g. PDB, slice 4) must supply a B-factor or `NaN`.
