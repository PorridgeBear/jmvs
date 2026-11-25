# 0008 Read mmCIF with rcsb ciftools-java

Status: Accepted. Supersedes legacy ADR-0004.

## Context
The legacy `CIFReader` splits rows on whitespace (breaking quoted values), ignores non-loop categories and multi-line values, dispatches by reflection, logs every row and calls `System.exit` on error (legacy debt D6). The `.cif.gz` sample cannot be read (D13).

## Decision
- Implement `MoleculeReader` in `jmvs-core` with **`org.rcsb:ciftools-java`** (MIT, Maven Central; 9.x at time of writing), using its typed mmCIF schema for `atom_site`, `struct_conf` (helices and turns) and `struct_sheet_range`.
- Detect gzip by magic bytes and wrap the stream in `GZIPInputStream`; ciftools also reads BinaryCIF (`.bcif`).
- Map ciftools data to the JMVS domain model in an adapter so no ciftools types leak out of `jmvs-core`. Chains are keyed by `label_asym_id`; polymer residues by `label_seq_id`, other residues (ligands, waters) by `auth_seq_id` and insertion code; only the first alternate location of each atom is kept.
- Pick the reader by content/extension through a small `StructureReaders` registry rather than hard-coding one.
- Parse failures raise `StructureReadException` with the file name and cause ([0011](0011-error-handling-and-logging.md)).

## Alternatives considered
- **Harden the in-house tokenizer**: no dependency, but CIF quoting rules and BinaryCIF are non-trivial to get right.
- **BioJava**: full structure toolkit; much larger dependency than needed (it uses ciftools-java itself).

## Consequences
- Correct handling of quoting, multi-line values and BinaryCIF.
- One more runtime dependency (with gson and javax.annotation-api); verified on Java 25.
- Atom counts are lower than the number of `ATOM`/`HETATM` lines for files with alternate locations (e.g. `1usc`: 3,285 of 3,415).
- The legacy `CIFReaderTest` expectations (1crn: 1 model, 1 chain, 46 residues, 327 atoms) carry over as golden values.
