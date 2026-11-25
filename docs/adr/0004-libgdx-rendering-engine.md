# 0004 libGDX (LWJGL3 backend) as the rendering engine

Status: Accepted. Supersedes legacy ADR-0001.

## Context
Java3D 1.x is unmaintained and ties JMVS to JDK 11 and vendored natives (legacy risks R1–R3). The renderer must run on macOS (Apple Silicon), Windows and Linux, come from Maven Central, support drawing tens of thousands of atoms smoothly, and let JMVS supply its own shaders.

## Decision
Use **libGDX** (1.14.x) with the **LWJGL3 desktop backend**, requesting an OpenGL 3.2 core context (`GL30` emulation) so instanced rendering is available on macOS (which caps at OpenGL 4.1). JMVS uses libGDX's `Mesh`, `ShaderProgram`, `PerspectiveCamera` and math types directly rather than the high-level `ModelBatch`/`ModelInstance` API, so atoms and bonds can be drawn as instances ([0007](0007-instanced-rendering-incremental-recolouring.md)). All GL code is confined to `jmvs-render-gdx` ([0002](0002-multi-module-build.md)).

## Alternatives considered
- **JavaFX 3D**: built-in UI, but no custom shaders or instancing; one node per atom repeats the legacy scaling problem.
- **jMonkeyEngine**: full scene graph and instancing, but heavier and game-engine oriented.
- **Raw LWJGL 3**: maximum control, but window, input, UI and math must all be written by hand.
- **Java3D 1.7 from Maven**: smallest change, but stays on a legacy scene-graph API.

## Consequences
- Cross-platform natives come from Maven Central (`gdx-platform` `natives-desktop`).
- Custom GLSL shaders are needed for instanced spheres, cylinders and lit ribbons.
- libGDX owns the window and main loop; the UI must also be libGDX ([0005](0005-scene2d-ui-native-file-dialog.md)).
- On macOS GLFW needs the first thread. `Lwjgl3ApplicationConfiguration.useGlfwAsync()` needs LWJGL's `glfw_async` native, which the LWJGL version libGDX depends on lacks, so JMVS passes `-XstartOnFirstThread` from the Gradle `run` task and the packaged macOS app, and the launcher restarts the JVM with that flag when started any other way (e.g. the generic start scripts).
- libGDX 1.14.2 depends on LWJGL 3.3.3, which warns about Java 25's JNI version; the app aligns all LWJGL modules to 3.3.6 with the LWJGL BOM (same 3.3 API line).
- libGDX does not adapt GLSL for a core-profile context, so `GlslCompatibility` sets `ShaderProgram.prependVertexCode`/`prependFragmentCode` to `#version 150` plus defines; all shaders (libGDX's and JMVS's) are written in GLSL ES 1.0 style.
- The JVM needs `--enable-native-access=ALL-UNNAMED` and `--sun-misc-unsafe-memory-access=allow` to load natives without warnings; start scripts and installers pass them.
