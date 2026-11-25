# 0012 Testing strategy

Status: Accepted

## Context
The legacy app has one reader test and a stub (legacy risk R8). The rewrite replaces parser, bonding and rendering, so regressions must be caught automatically.

## Decision
Tests use JUnit Jupiter (version from the catalog) and run in CI on every push.

| Level | Module | What |
|---|---|---|
| Golden-file | `jmvs-core` | For each `data/*.cif` (and `.cif.gz`): models, chains, residues, atoms, helix/sheet/turn residues, bounds and bond counts match committed expected values |
| Parity | `jmvs-core` | Spatial-grid bonding equals a reference all-pairs implementation of the RasMol rules ([0009](0009-spatial-grid-covalent-bonding.md)) |
| Unit | `jmvs-core`, `jmvs-scene` | Element table, readers' error handling, each display mode's instance counts and bounds, each colour scheme's colours, ribbon spline sanity |
| Performance | `jmvs-core` | Loading and bonding `1zpd` stays under a time budget (tagged, run in CI but tolerant of slow runners) |
| Unit | `jmvs-app` | `ViewerController`: open, mode/scheme switching, scheme fallback, invalid file keeps the current structure, selection |
| Smoke | `jmvs-app` | Manual: `jmvs <file> --mode <id> --scheme <id> --screenshot <png>` renders once and writes a PNG; `--benchmark <s>` logs the average frame rate |

- `jmvs-core` and `jmvs-scene` tests need no GPU or display.
- Sample files in `data/` are the single source of test inputs (no duplicated copies in resources).
- Visual parity with the legacy app is checked manually at cutover against the README screenshots.

## Consequences
- Refactors of the model, reader and bonding are safe.
- Rendering correctness relies on manual checks; automated screenshot tests can be added later.
