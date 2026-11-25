# JMVS architecture documentation

## Legacy (as-is) vs. to-be

| | As-is (legacy) | To-be |
|---|---|---|
| arc42 | [`arc42/legacy/`](arc42/legacy/) | [`arc42/`](#to-be-arc42) |
| ADRs | [`adr/legacy/`](adr/legacy/README.md) | [`adr/`](adr/README.md) |
| Plan | — | [`modernisation.md`](modernisation.md) |

The **legacy** docs describe the current Java3D-based system. They were reverse-engineered from source, build files and `README.md` ahead of the modernisation. Nothing in them was supplied by the original author unless stated; rationale not stated in the code or README is marked **[Inferred]** and unverified facts **[Assumption]**. Snapshot: commit `cae0dba`, ~3,900 lines of Java in `app/src`. Legacy docs are not edited, except for *Superseded by* lines on legacy ADRs.

The **to-be** docs describe the target architecture: Java 25, a multi-module Gradle 9 build, libGDX rendering with a Scene2D UI, ciftools-java for mmCIF, and native installers. [`modernisation.md`](modernisation.md) is the roadmap with phase status and maps each legacy risk and debt to its fix.

ADR numbering restarts at 0001 in `adr/`; legacy records are referred to as "legacy ADR-000n". Guidance for contributors and AI agents is in [`AGENTS.md`](../AGENTS.md).

## To-be arc42

| # | Section |
|---|---------|
| 01 | [Introduction and goals](arc42/01-introduction-and-goals.md) |
| 02 | [Constraints](arc42/02-constraints.md) |
| 03 | [Context and scope](arc42/03-context-and-scope.md) |
| 04 | [Solution strategy](arc42/04-solution-strategy.md) |
| 05 | [Building block view](arc42/05-building-block-view.md) |
| 06 | [Runtime view](arc42/06-runtime-view.md) |
| 07 | [Deployment view](arc42/07-deployment-view.md) |
| 08 | [Cross-cutting concepts](arc42/08-crosscutting-concepts.md) |
| 09 | [Architecture decisions](arc42/09-architecture-decisions.md) |
| 10 | [Quality requirements](arc42/10-quality-requirements.md) |
| 11 | [Risks and technical debt](arc42/11-risks-and-technical-debt.md) |
| 12 | [Glossary](arc42/12-glossary.md) |

## To-be ADRs

See [adr/README.md](adr/README.md).

## Legacy arc42

| # | Section |
|---|---------|
| 01 | [Introduction and goals](arc42/legacy/01-introduction-and-goals.md) |
| 02 | [Constraints](arc42/legacy/02-constraints.md) |
| 03 | [Context and scope](arc42/legacy/03-context-and-scope.md) |
| 04 | [Solution strategy](arc42/legacy/04-solution-strategy.md) |
| 05 | [Building block view](arc42/legacy/05-building-block-view.md) |
| 06 | [Runtime view](arc42/legacy/06-runtime-view.md) |
| 07 | [Deployment view](arc42/legacy/07-deployment-view.md) |
| 08 | [Cross-cutting concepts](arc42/legacy/08-crosscutting-concepts.md) |
| 09 | [Architecture decisions](arc42/legacy/09-architecture-decisions.md) |
| 10 | [Quality requirements](arc42/legacy/10-quality-requirements.md) |
| 11 | [Risks and technical debt](arc42/legacy/11-risks-and-technical-debt.md) |
| 12 | [Glossary](arc42/legacy/12-glossary.md) |

## Legacy ADRs

See [adr/legacy/README.md](adr/legacy/README.md).
