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
| [1bna.cif](https://www.rcsb.org/structure/1BNA) | B-DNA dodecamer (Drew–Dickerson) | 566 / 4 | Nucleic acid: two 12-nucleotide strands plus waters, for the Cartoon mode's backbone tube and base blocks. No secondary-structure annotation, so *Auto* computes (all coil). |
| [AF-P69905-F1-model_v6.cif](https://alphafold.ebi.ac.uk/entry/P69905) | AlphaFold DB prediction of human haemoglobin subunit alpha (UniProt P69905) | 1,077 / 1 | Predicted model: pLDDT in `B_iso_or_equiv` for the B-factor and Confidence schemes. Nearly all residues are very high confidence; Met 1 is low (65.38). Pairs with `4hhb`. From [AlphaFold DB](https://alphafold.ebi.ac.uk/), © DeepMind, [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). |
| AF-P69905-F1-model_v6-noss.cif | Same, without secondary-structure annotation | 1,077 / 1 | The `_struct_conf` and `_struct_conf_type` loops are deleted (lines 779–827 of the original), like a predicted model or file without annotation. Ribbons and Structure colouring must still show helices, computed from the backbone (Auto source, ADR 0018). |

The RCSB files don't mark any turns (`TURN_*` in `_struct_conf`); the PDB no longer annotates them. The AlphaFold
file's annotation comes from DSSP 4 and includes turns, 3-10, pi and polyproline helices and bends; it is the
reference for computed secondary structure
([`ComputedSecondaryStructureGoldenTest`](../jmvs-core/src/test/java/com/adcworks/jmvs/core/chem/ComputedSecondaryStructureGoldenTest.java)).
Turn handling in the reader is also tested with the synthetic file in `jmvs-core/src/test/resources`.

To add a file, download it from RCSB (`https://files.rcsb.org/download/<id>.cif` or
`https://models.rcsb.org/<id>.bcif`), add a row here and golden values to `SampleFilesGoldenTest`. AlphaFold DB
models are at `https://alphafold.ebi.ac.uk/files/AF-<uniprot>-F1-model_v<n>.cif` (the latest version is in
`https://alphafold.ebi.ac.uk/api/prediction/<uniprot>`); the file server returns 403 to clients without a
browser-like `User-Agent`, e.g. `curl -A "Mozilla/5.0"`.
