# 03 Context and scope

## Business context

```mermaid
flowchart LR
    U[User] -- mouse, menus, drag-and-drop --> J[JMVS]
    F[(mmCIF / BinaryCIF files<br/>.cif, .cif.gz, .bcif)] -- read --> J
    J -- 3D image, atom details, errors --> U
    PDB[wwPDB / RCSB] -. user downloads files manually .-> F
```

| Partner | Interaction |
|---|---|
| User | Opens files, chooses display mode and colour scheme, rotates/zooms/pans, picks atoms |
| Structure files | Local files chosen via the native dialog or dropped on the window; samples in `data/` |
| wwPDB / RCSB | Source of files and format definitions; no network access from the app |

## Technical context

```mermaid
flowchart LR
    JMVS --> GDX[libGDX 1.14<br/>LWJGL3 backend]
    GDX --> LWJGL[LWJGL 3 + GLFW natives]
    LWJGL --> GL[OpenGL 3.2+ driver]
    JMVS --> TINY[LWJGL tinyfd<br/>native file dialog]
    JMVS --> CIF[ciftools-java]
    JMVS --> CP[classpath resources<br/>properties, shaders, skin]
```

No network, database or inter-process interfaces. Inputs: files, mouse, keyboard. Outputs: on-screen rendering and log messages via `System.Logger`.
