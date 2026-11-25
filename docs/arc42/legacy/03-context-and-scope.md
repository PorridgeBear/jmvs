# 03 Context and scope

## Business context

```mermaid
flowchart LR
    U[User] -- mouse, menus --> J[JMVS]
    F[(mmCIF files<br/>e.g. data/*.cif)] -- read --> J
    J -- 3D image --> U
    PDB[wwPDB / RCSB] -. user downloads files manually .-> F
```

| Partner | Interaction |
|---|---|
| User | Loads files, picks mode/paint, rotates/zooms |
| mmCIF files | Local files chosen via `FileDialog`; samples in `data/` (1crn, 1usc, 1zpd, 6l1f; the `.gz` variant is not read by the loader) |
| wwPDB | Source of files and format spec; **no network access** from the app |

## Technical context

```mermaid
flowchart LR
    JMVS --> J3D[Java3D 1.x<br/>javax.media.j3d / com.sun.j3d.utils]
    J3D --> JOGL[JOGL + GlueGen natives]
    JOGL --> GL[OpenGL driver]
    JMVS --> AWT[AWT / Swing]
    JMVS --> CP[classpath resources<br/>jmvs.properties, elements.properties]
    JMVS --> FS[relative-path shaders<br/>src/main/resources/phong.*]
```

No network, database, or inter-process interfaces. Inputs: file dialog, mouse. Outputs: on-screen render, stdout (verbose logging).
