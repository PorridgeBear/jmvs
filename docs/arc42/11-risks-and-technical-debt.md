# 11 Risks and technical debt

Legacy risks and debts are listed in [legacy §11](legacy/11-risks-and-technical-debt.md); how each is resolved is tracked in [`../modernisation.md`](../modernisation.md#risk-and-debt-resolution). This section lists the risks of the target architecture and the migration.

## 11.1 Risks

| # | Risk | Impact | Mitigation | Status |
|---|---|---|---|---|
| T1 | macOS first-thread requirement for GLFW | App fails to start on macOS | `-XstartOnFirstThread` in `run` and the `.dmg`; launcher restarts the JVM otherwise ([ADR 0004](../adr/0004-libgdx-rendering-engine.md)) | Mitigated; verified with `run`, start scripts and the packaged app |
| T2 | VisUI or ciftools-java incompatible with libGDX 1.14 / Java 25 | UI or reader rework | Verified when introduced | Resolved; VisUI's version check skipped for libGDX 1.14.2 |
| T3 | Instanced shaders behave differently across GPU drivers | Rendering defects on some platforms | GLSL 1.50 core; manual checks on all three OSes | Partly mitigated: renders correctly on macOS (Apple GPU) and in CI on Windows and Linux (Mesa llvmpipe); not yet checked on Windows/Linux GPU drivers |
| T4 | Gradle cannot run on the newest local JDK | `./gradlew` fails for developers | Gradle 9.9 runs on JDK 17–27; daemon JVM criteria if a newer JDK appears; latest-JDK CI job ([ADR 0001](../adr/0001-java-25-gradle-9-toolchains.md)) | Mitigated |
| T5 | Visual regressions vs legacy (ribbons especially) | Loss of fidelity | Side-by-side comparison with `legacy-app` and README screenshots before cutover | Closed at cutover: README compares each legacy screenshot with the new app; `legacy-app` stays runnable for further comparison |
| T6 | Two JDKs and vendored jars during migration | Slower, more complex build | Time-boxed by [ADR 0003](../adr/0003-strangler-migration-legacy-module.md) | Resolved: `legacy-app` archived as a standalone build outside the root build and CI; its jars stay in the repository |
| T7 | Unsigned installers | Users blocked by OS security prompts | Workaround in the root `README.md`; consider signing later | Open |
| T8 | No automated rendering tests | Renderer regressions slip through | `--screenshot` for manual checks; release workflow renders `1crn` from each installer; screenshot comparison tests later | Open |
| T9 | LWJGL 3.3.6 overrides libGDX's tested 3.3.3 | Subtle backend incompatibilities | Same 3.3 API line; full manual run of all modes; revisit when libGDX updates LWJGL | Accepted |
| T10 | Windows `.msi` packaging depends on WiX on the runner | Release job fails | Release workflow installs WiX 5 with `dotnet tool`; dry runs on pull requests that touch packaging | Mitigated |

## 11.2 Accepted debt

| # | Debt | Reason |
|---|---|---|
| A1 | Menus are drawn in the window, not in the macOS system menu bar | Consequence of a single libGDX window ([ADR 0005](../adr/0005-scene2d-ui-native-file-dialog.md)) |
| A2 | No external plugin loading | Not needed now ([ADR 0006](../adr/0006-renderer-neutral-scene-and-display-mode-spi.md)) |
| A3 | Element radii remain RasMol values (not Cordero) | Parity with RasMol (legacy ADR-0003) |
| A4 | No level-of-detail for > 1M atoms | Beyond target scenarios |
| A5 | Installers bundle LWJGL natives for every platform (~10 MB) | Simpler build; could be filtered per OS |
| A6 | Ribbons are flat strips without arrowheads or tubes | Parity with the legacy ribbons |
