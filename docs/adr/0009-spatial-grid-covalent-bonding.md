# 0009 Spatial-grid covalent bonding with RasMol rules

Status: Accepted. Legacy ADR-0003 (RasMol-derived chemistry) remains in force.

## Context
Legacy `Model.createCovalentBonds` compares every atom pair (O(n²)) with `pow`/`sqrt` and prints a line per bond (legacy debt D1). The bonding rules themselves are ported from RasMol and should be kept.

## Decision
- Keep the RasMol rules exactly: for more than 1000 atoms the "fast" rule (bond if 0.4–1.9 Å, or ≤ 1.2 Å when hydrogen is involved), otherwise sum of covalent radii + 0.56 Å.
- Find candidate pairs with a **uniform spatial grid** (cell size = maximum bond distance), checking only the 27 neighbouring cells, comparing squared distances and bonding only within the same model.
- No per-bond logging; log a single summary at debug level.
- Verify parity: for each sample in `data/`, the set of bonds equals that of a reference all-pairs implementation of the same rules kept in test code.

## Consequences
- Bonding is ~O(n) and no longer dominates load time: reading and bonding `1zpd` takes ~0.1–0.2 s with a warm JVM.
- Results are unchanged from the legacy rules, protected by parity tests.
- The 255 vs 1000 threshold mismatch noted in legacy ADR-0003 is resolved in favour of the code (1000).
- Picking uses a simpler linear scan ([0014](0014-camera-interaction-and-picking.md)).
