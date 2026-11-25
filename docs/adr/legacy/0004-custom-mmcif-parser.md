# 0004 Custom line-based mmCIF parser

Status: Accepted (fragile)

## Context
README (2022): "The molecular loader has been replaced with an all-new loader for the latest PDBx/mmCIF type and loads atoms, sheets and helices." Earlier format support is not present in this repo.

## Decision
`CIFReader` (behind `MoleculeReader`) scans line by line, tracking only `_atom_site.*`, `_struct_conf.*`, `_struct_sheet_range.*` loop columns, splits rows on whitespace, and dispatches to the private method named after the category via reflection (`getDeclaredMethod`). Helices/sheets are applied after atoms; then `Molecule.finish()` computes bounds and bonds. No third-party mmCIF library is used.

## Consequences
- Small, dependency-free, enough for the sample files.
- Does not handle quoted/multi-line values, non-loop (key-value) categories, alt-locs, or validation; failures call `System.exit(0)`.
- Secondary structure is matched by `label_seq_id` only (see risk D2).
- Reflection dispatch hides control flow and relies on method names.
- Rationale for not using a library: **[Inferred]** learning/tinkering aim per README.
- Modernisation: adopt a maintained parser (e.g. BioJava/other) or harden this one behind the existing interface; keep `CIFReaderTest` as a regression baseline.
