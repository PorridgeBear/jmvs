# JMVS architecture documentation

## Legacy (as-is) vs. to-be

| | As-is (legacy) | To-be |
|---|---|---|
| arc42 | [`arc42/legacy/`](arc42/legacy/) | `arc42/` (reserved, not yet written) |
| ADRs | [`adr/legacy/`](adr/legacy/README.md) | `adr/` (reserved, not yet written) |

The legacy docs describe the current Java3D-based system. They were reverse-engineered from source, build files and `README.md` ahead of a modernisation effort. Nothing in them was supplied by the original author unless stated; rationale not stated in the code or README is marked **[Inferred]** and unverified facts **[Assumption]**. Target-state arc42 sections and new ADRs belong directly in `arc42/` and `adr/`.

Snapshot: single commit `cae0dba`, ~3,900 lines of Java in `app/src`.

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
