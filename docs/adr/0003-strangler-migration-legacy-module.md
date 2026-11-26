# 0003 Strangler migration with the legacy app kept as a module

Status: Completed (cutover in roadmap slice 0; legacy app archived, not deleted)

## Context
Java3D cannot run on Java 25, so the new app cannot reuse the legacy rendering code. The legacy app is the only reference for expected visuals (screenshots in the root `README.md`) and behaviour (legacy F1–F8).

## Decision
- Tag the current state as `legacy-java3d` before restructuring (done).
- Move `app/` unchanged to a `legacy-app/` Gradle module compiled and run on a **JDK 11 toolchain**, with its vendored jars, so it can still be run for side-by-side comparison on macOS.
- Build the new app in new modules. Port legacy logic (RasMol rules, ribbon maths) by rewriting, not by sharing code.
- When the new app reaches parity (F1–F8 and the quality targets in arc42 §10), delete `legacy-app/`, its jars and Sun-copyright sources, and mark this ADR *Completed*.

## Alternatives considered
- **Replace in place, rely on the tag**: less build complexity, but comparison needs a separate checkout and an old Gradle.

## Consequences
- Both apps can be run from one checkout during migration.
- The build temporarily needs two JDKs and still contains vendored jars (legacy risk R3) and Sun sources (R4).
- `legacy-app` is frozen: changes only to keep it building.

## Outcome
At cutover the owner chose to keep the legacy app in the repository for posterity instead of deleting it:
- `legacy-app/` is a **standalone Gradle build** with its own `settings.gradle.kts` and checksums. It is not part of the root build or CI; run it with `./gradlew -p legacy-app run` (macOS only).
- Its vendored jars (legacy risk R3) and Sun-copyright sources (R4) therefore remain, confined to `legacy-app/`; nothing in the new modules uses them.
- The root build needs only JDK 25; JDK 11 is provisioned only when the legacy build is run.
