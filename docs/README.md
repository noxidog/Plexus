# Plexus — Design Documentation

*A symmetry-routed exact classifier — a router, not a network.*

## Specification

| # | Document | § | Contents |
|---|---|---|---|
| — | [Design Overview](design.md) | §0–§1 | The task, the contradictions that make it hard, the domain frame, and what Plexus is. |
| 1 | [Architecture](01-architecture.md) | §7 | `Operation` as the spine, the package tree, and which concern owns what. |
| 2 | [Core Model](02-core-model.md) | §4–§5 | The composite `Key`, sectors and chain policy, the `Context`, one-pass training, the residual boundary. |
| 3 | [The Invariant Chain](03-invariants.md) | §3 | `Signature`, the invariance contract, the cell-graph reading, and minimality. |
| 4 | [The Symmetry Core](04-symmetry.md) | §2 | D4, fundamental domains, the canonical stabilizer, and group multiplication. |
| 5 | [The Trie](05-trie.md) | §5.3–§5.4 | Keys as paths, the lazy walk, and self-extension at insert. |
| 6 | [Search](06-search.md) | §6 | The `Searcher` sweep, the fibration, and `Fiber` enumeration. |
| 7 | [Descriptors](07-descriptors.md) | §6.2, §6.4, §9 | `Shape` as geometric identity, and decomposition as the stabilizer’s orbit partition. |
| 8 | [Diagnostics](08-diagnostics.md) | §3.5–§3.6 | The collision diagnostic, and the chain critic. |
| 9 | [API: the Interpreter](09-api.md) | §8 | Every REPL query, laid out on the descriptor’s own two axes. |
| 10 | [Implementation Status](10-implementation.md) | — | The open engineering TODOs, in one register. |

## Appendices

Material that is explanatory rather than specification — read the core set without it and nothing is missing; read it and the reasoning is visible.

| Appendix | Contents |
|---|---|
| [A — Mathematical Background](appendices/A-mathematical-background.md) | Frames as physics: the coherence argument, read as an interpretive lens. |
| [B — Design Rationale](appendices/B-design-rationale.md) | The seven principles, and what the cut-law dial really chooses. |
| [C — Rejected Designs](appendices/C-rejected-designs.md) | Backoff, and materializing the group algebra as trie levels. |
| [D — Performance Notes](appendices/D-performance-notes.md) | Path length as description length, and the fiber cost model. |
| [E — Formal Results](appendices/E-formal-proofs.md) | The bounding-box cap, plus a register of every result and where it lives. |
| [F — Worked Examples](appendices/F-examples.md) | The T decomposition, plus a register of the in-place examples. |
| [G — Complexus](appendices/G-history.md) | The forthcoming sibling project: invariant synthesis, kept downstream. |

## Reading orders

- **To use it** — [Design Overview](design.md) → [API](09-api.md) → `../data/README.md`.
- **To understand the mechanism** — [Core Model](02-core-model.md) → [Symmetry](04-symmetry.md) → [Invariants](03-invariants.md) → [Trie](05-trie.md).
- **To extend it** — [Invariants](03-invariants.md) → [Diagnostics](08-diagnostics.md) → [Appendix C](appendices/C-rejected-designs.md) → [Appendix G](appendices/G-history.md).
- **To judge it** — [Appendix B](appendices/B-design-rationale.md) → [Appendix E](appendices/E-formal-proofs.md) → [Appendix A](appendices/A-mathematical-background.md).
