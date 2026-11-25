# 0015 Typed configuration from classpath properties

Status: Accepted. Legacy ADR-0005 (properties-based configuration) remains in force; this ADR refines it.

## Context
Legacy configuration lives in `jmvs.properties` and `elements.properties` on the classpath. Values are parsed repeatedly (per atom, debt D9), some keys are ignored (`plugin.default.*`, `molecule`), and the element table is a static singleton (D11).

## Decision
- Keep classpath `.properties` files as the configuration format.
- Parse them **once at startup** into immutable typed objects: `ElementTable` (`jmvs-core`, from `elements.properties`), `ColourTables` (CPK, residue, structure, chain palette) and `ViewerConfig` (enabled modes/schemes by id and defaults), both in `jmvs-scene` from `jmvs.properties`. Files live next to the class that loads them on the classpath.
- Validate on load: unknown ids, malformed colours and missing elements raise `ConfigurationException` naming the key.
- Pass config objects to the code that needs them; no static state.
- Remove unused keys (`plugin.*`, `molecule`); every remaining key is read and documented in the file. Legacy residue colours are kept except Asn/Gln, which the legacy file set to yellow despite a "cyan" comment; they now use RasMol's cyan.

## Consequences
- Configuration errors are reported clearly at startup.
- Rendering code works with typed values, not strings.
- User-editable config outside the jar is not supported yet; can be added by layering a file over the defaults.
