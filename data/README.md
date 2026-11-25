# Sample structures

PDBx/mmCIF files from the [RCSB Protein Data Bank](https://www.rcsb.org/), used as test inputs and for trying the
viewer. Every file has golden values in
[`SampleFilesGoldenTest`](../jmvs-core/src/test/java/com/adcworks/jmvs/core/io/SampleFilesGoldenTest.java), and the
smaller ones are also checked by
[`CovalentBonderParityTest`](../jmvs-core/src/test/java/com/adcworks/jmvs/core/chem/CovalentBonderParityTest.java).

Counts are as loaded by JMVS: only the first alternate location of each atom is kept, and waters and ligands count
as chains (each has its own `label_asym_id`).

| File | Structure | Atoms / chains | Why it's here |
|---|---|---|---|
| [1crn.cif](https://www.rcsb.org/structure/1CRN) | Crambin | 327 / 1 | Small and quick. The legacy app's reader test used it (1 model, 1 chain, 46 residues, 327 atoms) and the new reader must give the same counts. Under 1,000 atoms, so it uses RasMol's radius-based bonding rule. |
| [1usc.cif](https://www.rcsb.org/structure/1USC) | Putative styrene monooxygenase small component | 3,285 / 8 | The structure in the README screenshots, 2022 and 2026. Two protein chains with helices and sheets, ligands, waters and some alternate locations. |
| 1usc.cif.gz | Same, gzip-compressed | 3,285 / 8 | Gzip reading: the 2022 loader couldn't read it. Must load identically to `1usc.cif`. |
| [1zpd.cif](https://www.rcsb.org/structure/1ZPD) | Pyruvate decarboxylase | 19,789 / 20 | The main performance benchmark (~20k atoms): load time, bonding speed and frame rate. |
| [6l1f.cif](https://www.rcsb.org/structure/6L1F) | PHF20L1 Tudor1 domain with a DNMT1 peptide | 667 / 3 | Small protein–peptide complex, so a short chain next to a longer one. |
| [4hhb.cif](https://www.rcsb.org/structure/4HHB) | Human deoxyhaemoglobin | 4,779 / 14 | Classic all-helix, four-chain protein with haem groups. Shows helices assigned per chain: the 2022 version matched them by residue number across all chains. |
| 4hhb.bcif | Same, in BinaryCIF | 4,779 / 14 | BinaryCIF reading. Must load identically to `4hhb.cif`. |
| [3nir.cif](https://www.rcsb.org/structure/3NIR) | Crambin at 0.48 Å resolution | 784 / 6 | Same protein as 1crn at very high resolution, with hydrogens and many alternate locations (534 alternate-location rows). Tests that alternates aren't duplicated, and covers hydrogens under the radius-based bonding rule. |
| [1d3z.cif](https://www.rcsb.org/structure/1D3Z) | Ubiquitin, NMR | 12,310 / 10 models | Multiple models (10 NMR conformers, all drawn overlaid), numbered from 1. Hydrogen-rich and over 1,000 atoms, so it covers RasMol's 1.2 Å hydrogen limit in the distance-based rule. |
| [1aon.cif.gz](https://www.rcsb.org/structure/1AON) | GroEL–GroES chaperonin with ADP | 58,870 / 35 | Stress test at ~59k atoms; kept compressed (1.4 MB). |

None of these files mark any turns (`TURN_*` in `_struct_conf`); the PDB no longer annotates them. Turn handling is
tested with the synthetic file in `jmvs-core/src/test/resources`.

To add a file, download it from RCSB (`https://files.rcsb.org/download/<id>.cif` or
`https://models.rcsb.org/<id>.bcif`), add a row here and golden values to `SampleFilesGoldenTest`.
