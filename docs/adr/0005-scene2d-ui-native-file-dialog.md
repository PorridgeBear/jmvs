# 0005 Scene2D UI with a native file dialog

Status: Accepted. Supersedes legacy ADR-0006.

## Context
The legacy UI is an AWT `Frame` with a `MenuBar` hosting a heavyweight `Canvas3D`, with application state held in `JMVSPanel`. libGDX ([0004](0004-libgdx-rendering-engine.md)) owns the GLFW window, so AWT/Swing cannot share it reliably (embedding AWT with GLFW on macOS is unsupported).

## Decision
- Build the UI in **libGDX Scene2D**, drawn as an overlay in the same window: a menu bar (Data, Display, Colour, Help), a status bar (molecule, mode, colour scheme, picked atom), and modal dialogs for errors and About.
- Use **VisUI** (Apache 2.0) for its skin, menu bar and dialogs. VisUI 1.5.9 targets libGDX 1.14.1; its version check is skipped for the 1.14.2 hotfix release. The current mode and colour scheme are marked with `>` in the menus; schemes the current mode cannot show are disabled.
- Open files with the OS-native dialog from LWJGL's **tinyfd** binding (version aligned with the LWJGL version libGDX uses); also accept drag-and-drop of files onto the window.
- Keep application state in a UI-independent `ViewerController` (current structure, mode, scheme, selection) that notifies a listener; it has no libGDX dependency and is unit-tested.
- The tinyfd dialog runs on its own thread so rendering continues while it is open.

## Alternatives considered
- **Swing hosting an LWJGL canvas** (`lwjgl3-awt`): native look, but fragile on macOS and not a supported libGDX backend.
- **JavaFX shell**: separate window/toolkit; same embedding issues.

## Consequences
- One toolkit, one window, one thread for UI and rendering; no heavyweight/lightweight mixing.
- Menus are not in the macOS system menu bar.
- UI layout and look are in our hands; VisUI reduces the work.
- Loading runs off the render thread and posts results back with `Gdx.app.postRunnable`.
