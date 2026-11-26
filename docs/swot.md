# SWOT analysis: JMVS vs. what structural scientists need in 2026

Compares what JMVS can do today (after the [modernisation](modernisation.md)) with what researchers, students and
teachers typically expect from a molecular viewer in 2026, to inform what to build next. "Typical expectations" are
based on widely used viewers such as Mol* (the RCSB/PDBe web viewer), UCSF ChimeraX, PyMOL, VMD and Jmol. They
describe the field in general, not a feature-by-feature audit of those tools.

## Status quo

### Features

| Area | JMVS today |
|---|---|
| Opening files | Native open dialog, drag-and-drop, command-line argument. Local files only; no download by PDB ID |
| Navigation | Rotate, zoom, pan, reset view |
| Selection | Click one atom: it is highlighted and its element, residue, chain and coordinates are shown in the status bar |
| Analysis | None: no distances, angles, contacts, hydrogen bonds or sequence view |
| Models | All models drawn overlaid (e.g. the 10 NMR conformers of `1d3z`); no model picker |
| Assemblies | Asymmetric unit only; no biological assemblies or crystal symmetry |
| Output | PNG screenshot and frame-rate benchmark via command-line options (`--screenshot`, `--benchmark`); no export from the UI |
| Scripting | None |
| Platforms | Desktop: macOS, Windows and Linux; CI builds, tests and smoke-tests the installers on all three. Installers bundle Java |
| Performance | 59k atoms at 43 fps in balls mode, 211 fps in ribbons (Apple Silicon, see [arc42 §10](arc42/10-quality-requirements.md)) |

### Display modes

| Mode | Notes |
|---|---|
| Balls | Space-filling, van der Waals radii (RasMol united-atom radii without hydrogens) |
| Sticks | Bond cylinders with small joints; unbonded atoms (waters, ions) as dots |
| Balls and sticks | Quarter-size balls with thinner bonds |
| Ribbons | Flat Carson–Bugg ribbons through protein Cα atoms. No sheet arrows, no helix tubes, no nucleic acid backbones |

Not available: cartoon with arrows/cylinders, molecular surfaces, backbone trace, lines/wireframe, per-selection
mixed representations, labels.

### Colour schemes

| Scheme | Notes |
|---|---|
| CPK | By element (RasMol colours) |
| Chain | Fixed 12-colour palette, stable between runs |
| Group | By residue type (RasMol amino colours) |
| Structure | Helix, sheet, turn, coil, as annotated in the file; not computed (no DSSP) |

Not available: B-factor, AlphaFold confidence (pLDDT), hydrophobicity, charge or electrostatic potential, sequence
position (rainbow), custom colours, colour-blind-safe palettes.

### File formats

| Format | Status |
|---|---|
| PDBx/mmCIF (`.cif`, `.mmcif`) | Supported (ciftools-java) |
| Gzip-compressed (`.cif.gz`) | Supported |
| BinaryCIF (`.bcif`) | Supported; RCSB's preferred compressed format since MMTF was retired in July 2024 |
| Legacy PDB (`.pdb`) | Not supported |
| Small molecules (SDF, MOL2) | Not supported |
| Density maps (MRC/CCP4) | Not supported |
| MD trajectories (XTC, DCD) | Not supported |

## What scientists typically need in 2026

- **Predicted structures.** AlphaFold and similar models are everyday inputs. Users expect colouring by confidence
  (pLDDT, which these files store in the B-factor field) and, for complexes, a view of predicted aligned error.
- **Large and cryo-EM structures.** Ribosomes, viruses and other assemblies with hundreds of thousands to millions of
  atoms, often only available as mmCIF/BinaryCIF, together with their EM density maps.
- **Biological context.** Biological assemblies rather than the asymmetric unit, and ligand and binding-site views
  (nearby residues, interactions, hydrogen bonds).
- **Fetch by ID.** Typing an identifier (PDB, AlphaFold DB) instead of downloading files by hand.
- **Publication-quality output.** High-resolution images with good lighting (ambient occlusion, outlines), labels,
  and movies.
- **Selection and measurement.** A selection language or picker, distances and angles, and labels.
- **Reproducibility and sharing.** Saved sessions or scripts, and sharing views with others, often in the browser.
- **Nucleic acids and modified residues.** Cartoon representations for DNA/RNA, sensible handling of non-standard
  residues.
- **Teaching.** A simple, quick, free tool that students can install, with clear representations and few options.

## SWOT

| | Helpful | Harmful |
|---|---|---|
| **Internal** | **Strengths**<br>• Fast: instanced rendering handles ~60k atoms smoothly; loading and bonding take a fraction of a second<br>• Current file formats: mmCIF, gzip and BinaryCIF via RCSB's own library; no reliance on the retired MMTF or the frozen PDB format<br>• RasMol-faithful bonding and colours, checked by tests: familiar to people who learnt with RasMol<br>• Simple, uncluttered UI that suits teaching<br>• Works offline; installers bundle Java for macOS, Windows and Linux<br>• Clean, tested and documented architecture: new display modes and colour schemes are one class each<br>• Open source and free | **Weaknesses**<br>• No analysis: no measurements, contacts, hydrogen bonds or sequence view<br>• Only four representations; ribbons are flat strips with no arrows or tubes, and nothing for nucleic acids<br>• No surfaces<br>• No selection beyond a single atom; no labels<br>• Secondary structure only if the file annotates it; no DSSP<br>• Models overlaid with no picker; no biological assemblies<br>• No B-factor or pLDDT colouring<br>• No fetch by ID, sessions, scripting or image export from the UI<br>• Desktop only; single maintainer |
| **External** | **Opportunities**<br>• pLDDT/B-factor colouring is cheap to add (one field in the reader, one colour scheme) and makes AlphaFold models useful<br>• Fetch by PDB or AlphaFold DB ID: ciftools-java already supports reading by ID<br>• Biological assemblies from `pdbx_struct_assembly` would show proteins as they function<br>• DSSP-style secondary structure would make ribbons work on any file, including predictions<br>• Cartoon arrows, helix tubes and nucleic acid ribbons, built on the existing ribbon code<br>• A teaching niche: a fast, simple, RasMol-like desktop viewer with installers<br>• The renderer-neutral scene model would let a web or other renderer be added later | **Threats**<br>• Mol* is free, in the browser and built into the RCSB and PDBe sites; ChimeraX and PyMOL dominate desktop research use<br>• Expectations of scripting and reproducible sessions favour existing tools<br>• Structure sizes keep growing; millions of atoms would need level-of-detail work<br>• Apple has deprecated OpenGL on macOS. libGDX's ANGLE (Metal) route currently only offers OpenGL ES 2.0 on macOS, but JMVS's instanced rendering needs ES 3<br>• Dependency coupling: libGDX ships an older LWJGL than JMVS needs on Java 25<br>• One maintainer: limited capacity to keep pace |

## Gaps, ranked

Effort is relative (S/M/L). Value reflects how often the need comes up for the users above.

| Gap | Value | Effort | Notes |
|---|---|---|---|
| B-factor / pLDDT colour scheme | High | S | Read `B_iso_or_equiv`; add a colour scheme with the AlphaFold confidence bands |
| Fetch by PDB / AlphaFold DB ID | High | S | File > Open ID…; ciftools-java `readById` or a plain HTTPS download |
| Model picker | Medium | S | Show one model at a time, or all overlaid |
| Image export from the UI | Medium | S | Reuse the `--screenshot` code; optionally render larger than the window |
| Distance and angle measurement | High | M | Builds on picking; draw measurement lines and labels |
| Computed secondary structure (DSSP) | High | M | Needed for predicted models and files without annotation |
| Cartoon arrows, helix tubes, nucleic acid ribbons | High | M | Extends `RibbonsMode` |
| Biological assemblies | High | M | Apply `pdbx_struct_oper_list` transforms; instancing makes copies cheap |
| Selection (by chain, residue, ligand) and per-selection representations | High | L | Needs a selection model and scene composition |
| Molecular surfaces | High | L | Surface generation (e.g. marching cubes on a distance field) |
| Ambient occlusion and outlines | Medium | M | Screen-space effects in the renderer |
| Legacy PDB format reader | Medium | S | Still common in teaching material and older tools |
| Density maps (MRC/CCP4) | Medium | L | Isosurfaces from volumetric data |
| Sessions / scripting | Medium | L | Save and restore state; a small command language or Python bridge |
| Metal-capable rendering path on macOS | Medium | L | Watch libGDX ANGLE support for ES 3; fallback without instancing otherwise |

## Suggested direction

JMVS is unlikely to compete head-on with Mol* or ChimeraX. It can be good at being a fast, free, simple desktop
viewer for teaching and quick inspection, including of AlphaFold models.

The quickest gains are the small, high-value items: pLDDT/B-factor colouring, fetch by ID, a model picker and image
export. The medium items (DSSP, cartoon, measurements, assemblies) would then cover most everyday viewing needs.
These are ordered into feature slices in [roadmap.md](roadmap.md).
