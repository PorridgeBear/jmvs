# 0011 Error handling and logging

Status: Accepted

## Context
The legacy code calls `System.exit(0)` on configuration and parse errors, uses `printStackTrace`, and logs every parsed row and bond to stdout (legacy risk R6, cross-cutting §8). Errors terminate the app silently and cannot be tested.

## Decision
- Library code (`jmvs-core`, `jmvs-scene`, `jmvs-render-gdx`) never calls `System.exit`, `System.out` or `printStackTrace`.
- Failures are reported with checked or unchecked **typed exceptions** (`StructureReadException`, `ConfigurationException`) that carry context (file, line/category where known).
- The app catches failures at the boundary (load task, startup) and shows them in a dialog; the app keeps running with its previous state.
- Log with **`java.lang.System.Logger`** (no logging dependency). Default level INFO; per-item output only at TRACE.
- Only the launcher may exit the JVM, on unrecoverable startup failure, after logging.

## Consequences
- Invalid files no longer kill the app (quality scenario Q6).
- Error paths are testable.
- `System.Logger` defaults to `java.util.logging`; a different backend can be plugged in later without code changes.
