# 12 Glossary

| Term | Meaning |
|---|---|
| arc42 | Architecture documentation template |
| ADR | Architecture Decision Record |
| Java3D | Java scene-graph 3D API (`javax.media.j3d`, `com.sun.j3d.utils`) |
| JOGL / GlueGen | Java OpenGL binding and its native-binding generator, used by Java3D 1.7 |
| Scene graph | Tree of nodes (`BranchGroup`, `TransformGroup`, `Shape3D`) that Java3D renders |
| RasMol | Classic C molecular viewer JMVS imitates |
| PDB / PDBx/mmCIF | Protein Data Bank and its CIF-based structure file format |
| `_atom_site`, `_struct_conf`, `_struct_sheet_range` | mmCIF categories for atoms, helices (conformations), and sheet strands |
| Model / Chain / Residue / Atom | Structure hierarchy (model = e.g. NMR conformer; chain = `label_asym_id`) |
| Heterogen / HETATM | Non-standard residue/ligand/water atoms |
| CPK | Element-based colouring scheme |
| Cα (alpha carbon) | Backbone atom used as ribbon guide point |
| Balls / Sticks / Ribbons | Space-filling, bond-cylinder, and cartoon display modes |
| Paint mode | Colouring scheme: CPK, Chain, Group (residue), Structure |
| United-atom radii | Larger C/N/O/S radii used when no hydrogens are present (RasMol) |
| Plugin | Implementation of `JMVSPlugin` supplying a display mode |
