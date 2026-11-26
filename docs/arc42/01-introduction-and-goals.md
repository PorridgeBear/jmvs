# 01 Introduction and goals

## 1.1 Requirements overview

JMVS is a single-user desktop viewer for 3D macromolecular structures in the spirit of RasMol. The modernised app keeps the legacy capabilities ([legacy §1.1](legacy/01-introduction-and-goals.md)) and completes the unfinished ones.

| ID | Capability | Change from legacy |
|----|-----------|--------------------|
| F1 | Open a PDBx/mmCIF file via a native dialog or drag-and-drop | `.cif.gz` and BinaryCIF also accepted; loading does not block the UI |
| F2 | Read atoms, helices, sheets and turns | Turns added; robust parsing ([ADR 0008](../adr/0008-mmcif-via-ciftools-java.md)) |
| F3 | Infer covalent bonds with RasMol's rules | Same rules, spatial-grid search ([ADR 0009](../adr/0009-spatial-grid-covalent-bonding.md)) |
| F4 | Display as Balls, Sticks, Balls-and-sticks, Ribbons | Balls-and-sticks enabled |
| F5 | Colour by CPK, Chain, Group, Structure, B-factor, Confidence (pLDDT) | Any scheme with any supported mode; stable chain colours; turns coloured; pLDDT in the AlphaFold DB bands, with a legend |
| F6 | Rotate, zoom, pan, reset view | Pan and reset added |
| F7 | Switch mode/colour at runtime; status bar | Recolouring without rebuilding geometry |
| F8 | Pick an atom and show its details | Implemented ([ADR 0014](../adr/0014-camera-interaction-and-picking.md)) |
| F9 | Show readable errors and keep running | New ([ADR 0011](../adr/0011-error-handling-and-logging.md)) |
| F10 | Show a solvent-excluded or solvent-accessible surface over any display mode, coloured by the current scheme, opaque or transparent | New; computed in the background ([ADR 0017](../adr/0017-molecular-surfaces.md)) |

## 1.2 Quality goals

| Priority | Goal | Scenario (see [10](10-quality-requirements.md)) |
|---|---|---|
| 1 | Performance: smooth interaction with ≥ 10k-atom structures | Q2, Q3 |
| 2 | Portability: macOS, Windows, Linux on x64 and ARM, current LTS Java | Q4 |
| 3 | Maintainability: renderer isolated, core testable without a GPU | Q7, Q8 |
| 4 | Correctness: chemistry matches RasMol rules | Q5 |
| 5 | Robustness: bad input never terminates the app | Q6 |

## 1.3 Stakeholders

| Role | Expectation |
|---|---|
| Author/maintainer | A maintainable, buildable project to keep tinkering with on current hardware and Java |
| Contributors and AI agents | Clear module boundaries, conventions ([`AGENTS.md`](../../AGENTS.md)) and decisions |
| Users (students/hobbyists) | Install and view PDB structures without setting up Java |

## 1.4 Scope of the modernisation

See [`../modernisation.md`](../modernisation.md) for phases and status. Out of scope: network download from RCSB, editing structures, scripting, PDB (legacy format) files, code signing.
