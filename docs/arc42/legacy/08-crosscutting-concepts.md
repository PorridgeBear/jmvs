# 08 Cross-cutting concepts

| Concept | Implementation | Observations |
|---|---|---|
| Configuration | `jmvs.properties` (plugins, CPK `cpk.N`, residue colours `residue.xxx`), `elements.properties` (symbol, covalent r, vdW r, CPK index, name); loaded through the context class loader | Colours are `"r,g,b"` strings parsed on every atom; some keys (`plugin.default.*`, `molecule`) unused |
| Colouring | Per-plugin `getMaterial` duplicates logic: CPK, residue (Group), Chain (`AppearanceManager.getOrAllocateColour`, random colours cached in a static map — differ per run), Structure (hard-coded RGB for helix/sheet/turn) | Duplication across Balls/Sticks/Ribbons |
| Units/scaling | Coordinates in Å scaled by `0.1` for placement; sphere radii × 10 (`RADII_SCALE_FACTOR`); element radii in RasMol units (1 Å = 250) | Unit handling is implicit and scattered |
| Scene-graph capabilities | Every mutable node sets `ALLOW_*` capability bits explicitly | Java3D-specific boilerplate |
| Logging | `System.out.println` everywhere, including one line per bond and per parsed row | Dominates cost for large files |
| Error handling | `printStackTrace`, `System.exit(0)` | See 11 |
| Global state | `ElementsManager` static map/singleton; static colour map | Not thread-safe; tests share state |
| Threading | Everything on AWT/main thread; Java3D renders on its own threads | Loading blocks the UI |
| Reflection | Plugin instantiation (`Class.forName().newInstance()`), mmCIF category dispatch (`getDeclaredMethod`) | Fragile under modules/obfuscation |
| Testing | See 07 | Minimal |
| Security | Reads user-chosen local files only; no network | Class names for reflection come from bundled properties only |
