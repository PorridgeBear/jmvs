# 10 Quality requirements

No formal requirements exist; the scenarios below are inferred or proposed baselines **[Assumption]** for modernisation.

## Quality tree

- **Performance**: interactive frame rate and load time on large structures
- **Maintainability**: replaceable renderer, testable core
- **Portability**: macOS/Windows/Linux, x86/ARM
- **Correctness**: parity with RasMol chemistry
- **Extensibility**: new display/colour modes

## Scenarios

| ID | Scenario | Current evidence | Status |
|----|----------|------------------|--------|
| Q1 | Load `1crn.cif` (327 atoms) and display in < 2 s | Covered functionally by `CIFReaderTest`; timing not measured | Unknown |
| Q2 | Load a ≥ 10k-atom structure (e.g. `data/1usc.cif`, `data/6l1f.cif`) and rotate smoothly | README: slow beyond ~1000 atoms; one `TransformGroup` + `Sphere` per atom; O(n²) bonding with per-bond println | Likely failing |
| Q3 | Add a new display mode without modifying core code | Needs a class and a properties entry; menu auto-built. Third-party jar drop-in not supported | Partial |
| Q4 | Run on Windows/Linux | Only macOS natives vendored | Failing |
| Q5 | Bond counts match RasMol for a sample | Rules ported; only `totalBonds() > 0` asserted | Unverified |
| Q6 | Invalid file shows a message, app stays running | `System.exit(0)` on parse errors | Failing |
| Q7 | Core (`io`, `model`) testable without GPU | Yes — no Java3D imports there; only CIF read is tested | Partial |
