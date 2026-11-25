# 10 Quality requirements

## Quality tree

- **Performance**: load time, frame rate, responsiveness when switching mode/colour
- **Portability**: macOS, Windows, Linux; installer without a JDK
- **Maintainability**: renderer isolated; core and scene testable without a GPU; easy to add modes
- **Correctness**: RasMol-equivalent bonding; correct secondary structure per chain
- **Robustness**: bad input reported, app keeps running

## Scenarios

Reference machine: Apple M1-class Mac, integrated GPU, 2560×1504 back buffer (Retina). Samples are in `data/`. Frame rates are measured with `jmvs <file> --mode <id> --scheme chain --benchmark 5` (vsync off, continuous rotation).

| ID | Scenario | Target | Legacy | Result (2026-11, macOS) |
|----|----------|--------|--------|--------------------------|
| Q1 | Open `1crn.cif` (327 atoms) | Rendered < 1 s after choosing the file | Unknown | Met: read and bonded in ~70 ms on first load |
| Q2 | Open `1zpd.cif` (19,789 atoms) in Balls mode and rotate | Rendered < 3 s; rotation ≥ 60 fps | Likely failing | Met: loaded in 0.4–0.8 s (cold JVM); Balls 96 fps, Balls-and-sticks 108, Sticks 131, Ribbons 268. Stress test `1aon` (58,870 atoms): Balls 43 fps, Ribbons 211 |
| Q3 | Switch colour scheme on `1zpd` | < 100 ms, no geometry rebuild | Full rebuild | Met by design: only colour buffers are rewritten ([ADR 0007](../adr/0007-instanced-rendering-incremental-recolouring.md)); not timed separately |
| Q4 | Install and run on Windows and Linux | Installer from CI starts and loads `1crn` | Failing (macOS natives only) | Open: CI builds and tests on all three OSes; installers not yet run on Windows/Linux |
| Q5 | Bond sets match RasMol rules | Spatial-grid bonds equal reference all-pairs bonds for all samples | Unverified | Met: `CovalentBonderParityTest` |
| Q6 | Open an invalid or truncated file | Error dialog naming the file; previous structure still shown | Failing (`System.exit`) | Met: `CifStructureReaderTest`, `ViewerControllerTest` |
| Q7 | Run core and scene tests on a machine without a GPU | All pass in CI | Partial | Met: no GL dependency in `jmvs-core`/`jmvs-scene` tests |
| Q8 | Add a new display mode | One class + one registry entry; no changes to renderer or UI | Partial | Met: `DisplayMode` + `Registry`; menus are built from the registry |
| Q9 | Bonding and loading `1zpd` (excl. rendering) | < 1 s | O(n²) | Met: ~0.1–0.2 s warm (`LoadPerformanceTest`, budget 3 s for CI) |
| Q10 | Multi-chain helix/sheet assignment | Secondary structure applied only to the named chain | Incorrect (D2) | Met: `CifStructureReaderTest` |
