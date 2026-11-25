# 0006 AWT frame UI hosting Canvas3D

Status: Accepted (legacy)

Superseded by [ADR 0005](../0005-scene2d-ui-native-file-dialog.md).

## Context
Java3D's `Canvas3D` is an AWT heavyweight component. The project began in 1998/99 when AWT was the baseline.

## Decision
`App extends Frame`, `JMVSPanel extends Panel`, AWT `MenuBar`/`Menu`/`MenuItem`, `FileDialog`; a Swing `JLabel` status bar is mixed in. Canvas is added at the centre.

## Consequences
- Native menu bar/file dialog work well on macOS.
- Heavyweight/lightweight mixing can cause z-order problems.
- UI logic, state (molecule, plugin, paint) and orchestration all live in `JMVSPanel`; no separation of presentation and application logic.
- `System.exit(0)` on window close; no resize-aware layout work beyond `BorderLayout`.
- A replacement renderer will dictate the UI toolkit (e.g. JavaFX or Swing + GL canvas).

Rationale **[Inferred]**: AWT compatibility with Java3D.
