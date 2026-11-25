# 01 Introduction and goals

## 1.1 Requirements overview

JMVS (Java3D Molecular Visualisation System) is a single-user desktop viewer for 3D macromolecular structures, in the spirit of RasMol (`README.md`: "a RasMol-like 3D molecular viewer in Java"). Originated as a 1998/99 dissertation, refactored to a plugin design in 2003, revived in 2022 (Gradle build, mmCIF loader, M1 support).

Functional capabilities evidenced in code:

| ID | Capability | Evidence |
|----|-----------|----------|
| F1 | Load a PDBx/mmCIF file via a native file dialog (menu *Data > Load*) | `JMVSFileLoader`, `CIFReader` |
| F2 | Parse atoms (`_atom_site`), helices (`_struct_conf`), sheets (`_struct_sheet_range`) | `CIFReader` |
| F3 | Infer covalent bonds using RasMol's distance rules | `Model.createCovalentBonds` |
| F4 | Render in modes **Balls**, **Sticks**, **Ribbons** (Balls-and-Sticks exists but is disabled in `jmvs.properties`) | `plugins/*` |
| F5 | Colour by CPK (element), Chain, Group (residue), Structure (helix/sheet/turn); Ribbons lacks CPK | `paintModes` arrays in plugins |
| F6 | Rotate and zoom with the mouse | `Rotator`, `Zoomer` (Java3D `MouseRotate`/`MouseZoom`) |
| F7 | Switch mode/paint at runtime via *Plugins* menu; status line shows molecule/mode/paint | `JMVSPanel` |
| F8 | Atom picking | `Picker` exists but is commented out in `RenderScene` — **not functional** |

## 1.2 Quality goals

No explicit quality goals are documented. Inferred from README and code **[Inferred]**:

| Priority | Goal | Notes |
|---|---|---|
| 1 | Visual fidelity relative to RasMol (radii, bonding, colours copied from RasMol) | README, `elements.properties` header |
| 2 | Extensibility of display modes (plugin idea; "never took this idea to its full extent") | `JMVSPlugin`, README 2003 |
| 3 | Interactive performance | README: "Performance was an issue once atom count got to ... around 1000" — a known weakness, not an achieved goal |
| 4 | Buildability/runnability on current hardware | README 2022 section |

## 1.3 Stakeholders

| Role | Expectation |
|---|---|
| Author/maintainer (Allistair Crossley) | Preserve the project, keep it running, learn/tinker |
| Future modernisation team | Understand structure, Java3D coupling and debt (this documentation) |
| Users (students/hobbyists) **[Assumption]** | View PDB/mmCIF structures |

## 1.4 Modernisation goals (context for these docs)

Not stated in code; requested for this documentation effort: replace the unmaintained Java3D/JOGL-1.x-era stack and legacy Java style while preserving the functional capabilities F1–F7. See [11 Risks and technical debt](11-risks-and-technical-debt.md) for the Java3D dependency inventory.
