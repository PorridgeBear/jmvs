# 0014 Camera interaction and CPU ray picking

Status: Accepted

## Context
Legacy rotation and zoom extend Java3D mouse behaviours; picking (F8) exists but is disabled. Camera centring is computed from incorrect bounds (legacy debts D4, D5).

## Decision
- An **orbit camera** around the structure's bounding-sphere centre: left-drag rotates (arcball), scroll/right-drag zooms, middle-drag or shift-drag pans, a "Reset view" action re-frames the structure. Framing uses the bounding sphere so the whole structure fits regardless of orientation.
- **Picking** on click: unproject a ray from the camera and test it against the current scene's pick targets (`Picking` in `jmvs-scene`), taking the nearest hit. Pick targets are the drawn spheres for Balls and Balls-and-sticks, 0.6 Å spheres for Sticks, and 1.5 Å spheres on Cα atoms for Ribbons. A linear scan is fast enough for clicks on ~20k atoms, so no spatial index is used.
- The picked atom is highlighted ([0007](0007-instanced-rendering-incremental-recolouring.md)) and described in the status bar (element, atom name, residue, chain, sequence number, coordinates).
- Input handling (`OrbitCameraController`, `ScreenPicker`) sits in `jmvs-render-gdx`; the UI receives the selection through the app controller. Keyboard: `R` resets the view, `Esc` clears the selection, Ctrl/Cmd+O opens a file.

## Consequences
- Picking works without GPU read-back and is unit-testable.
- Picking accuracy follows the display mode's radii.
