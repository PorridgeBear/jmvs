# 0003 RasMol-derived chemistry and colours

Status: Accepted

## Context
JMVS is meant to be a RasMol-like viewer. README: RasMol's C source was studied; the 2022 update "improved the auto-covalent bond routine to match RasMol's implementation".

## Decision
Port RasMol conventions:
- Element table (covalent/vdW radii in RasMol units 1 Å = 250, CPK colour index) from RasMol's `abstree.h` (`elements.properties` header).
- Bond inference (`Model.createCovalentBonds`): "fast" rule (0.4–1.9 Å, 1.2 Å with H) when total atoms > 1000, else sum of covalent radii + 0.56 Å (comment cites an inconsistent 255 threshold; code uses 1000).
- United-atom radii for C/N/O/S when the file has no hydrogens (`JMVSPluginBalls`).
- CPK and residue colour tables in `jmvs.properties`.

## Consequences
- Familiar output for RasMol users; reasoning documented in links to the RasMol docs.
- Radii/units are legacy (`TODO update with Cordero values`).
- Code/comment threshold mismatch (255 vs 1000) — verify intended behaviour **[Inferred: code is authoritative]**.
- Bond detection is O(n²) (see risk D1); only `totalBonds() > 0` is tested.
