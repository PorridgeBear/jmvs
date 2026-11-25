# 0005 Properties-based configuration

Status: Accepted

## Context
The plugin refactor aimed at being "more configurable" (README 2003). The app needs plugin lists, colour tables and element data.

## Decision
Plain `java.util.Properties` files on the classpath (`jmvs.properties`, `elements.properties`), read through the context class loader. Colours are `"r,g,b"` strings; elements are comma-separated tuples.

## Consequences
- Easy to edit without recompiling.
- Stringly typed, no schema/validation; parsing repeated per use (see D9).
- Missing/empty files cause `System.exit(0)`.
- Static singleton `ElementsManager` holds element state.
- Some documented keys are unused (`plugin.default.*`, `molecule`).

Rationale **[Inferred]**: simplest option in the 2003-era Java standard library.
