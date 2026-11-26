# 12 Glossary

Terms from the [legacy glossary](legacy/12-glossary.md) still apply (mmCIF categories, Model/Chain/Residue/Atom, CPK, Cα, RasMol, display modes). Additional and changed terms:

| Term | Meaning |
|---|---|
| libGDX | Cross-platform Java game/graphics framework; JMVS uses its LWJGL3 desktop backend |
| LWJGL 3 / GLFW | Java bindings to OpenGL and the GLFW windowing library, used by libGDX |
| tinyfd | Tiny File Dialogs, native open/save dialogs via LWJGL |
| Scene2D / VisUI | libGDX UI toolkit / a widget and skin library for it |
| BinaryCIF | Binary encoding of mmCIF (`.bcif`) readable by ciftools-java |
| ciftools-java | RCSB library for reading and writing CIF/BinaryCIF |
| Toolchain | JDK selected by Gradle for compiling/running, independent of the JDK running Gradle |
| Instanced rendering | Drawing many copies of one mesh in one draw call with per-instance data |
| Scene (JMVS) | Renderer-neutral data produced by a display mode: sphere/cylinder instances and meshes |
| Display mode | How atoms are drawn (Balls, Sticks, Balls-and-sticks, Ribbons, Cartoon); replaces legacy "plugin" |
| Cartoon | Backbone drawn as helix ribbons, arrowed strands and coil tubes, with nucleic-acid backbones and base blocks |
| Superellipse | Curve `\|x/w\|^e + \|y/h\|^e = 1`: an ellipse for e = 2, nearer a rectangle as e grows; the cartoon cross-section |
| Colour scheme | How atoms are coloured (CPK, Chain, Group, Structure, B-factor, Confidence); replaces legacy "paint mode" |
| pLDDT | Predicted local distance difference test: AlphaFold's per-residue confidence (0–100), stored in the B-factor column |
| Bounding sphere | Smallest enclosing sphere (approximate) used to frame the camera |
| Spatial grid | Uniform 3D cell grid for fast neighbour search (bonding, surface colouring) |
| SES / SAS | Solvent-excluded (Connolly) surface: what a rolling solvent probe cannot reach. Solvent-accessible surface: the path of the probe's centre |
| Probe | Sphere standing in for a solvent molecule, 1.4 Å for water |
| Distance transform | Distance from every grid point to the nearest point of a set, computed in linear time |
| Surface nets | Meshing a level set with one vertex per crossed grid cube (a dual of marching cubes) |
| Icosphere | Sphere mesh made by subdividing an icosahedron; the instanced atom template |
| Strangler migration | Building the new system next to the old one and removing the old one at parity |
| DSSP | Define Secondary Structure of Proteins (Kabsch & Sander, 1983): assigns helices, strands and turns from backbone hydrogen bonds; JMVS computes a subset of it ([ADR 0018](../adr/0018-computed-secondary-structure.md)) |
| Turn | Secondary structure element from `_struct_conf` with a `TURN_*` type |
| jlink / jpackage | JDK tools that build a trimmed runtime / native installers |
