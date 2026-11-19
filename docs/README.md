# Plexus — Design Documentation

*A symmetry-routed exact classifier: it files every shape by what it is, not where it sits.*

| § | Document | Contents |
|---|---|---|
| 0–1 | [Design Overview](design.md) | The task, the contradictions that make it hard, and the shape of the key. |
| 2 | [The Descriptor](01-descriptor.md) | Bounding box, lattice pick, the symmetry walk, the canonical pose, invariants and trailing coordinates. |
| 3 | [The Trie](02-trie.md) | Two tries, training, the four verdicts, and the extrapolation setting. |
| 4 | [Self-Extension](03-extension.md) | A new shape coordinate where different shapes collide; the primitive battery; diagnose. |
| 5 | [Slices](04-slices.md) | Constraints per level, the callback, and constructive generation. |
| 6 | [The Interpreter](05-cli.md) | `cli.Main` and the data-file format. |
| 7 | [The Engine](06-engine.md) | Lazy grids, templates matched by containment in every pose, expressions, stages and pipelines. |

## Appendices

Interpretation, rationale and evidence, kept apart from the operating spec so the spec stays lean.

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

- **To use it:** [Design Overview](design.md) → [The Interpreter](05-cli.md) → `../data/README.md`.
- **To understand the key:** [The Descriptor](01-descriptor.md) → [The Trie](02-trie.md).
- **To query and generate:** [The Trie](02-trie.md) → [Slices](04-slices.md).
- **To see how it grows:** [The Trie](02-trie.md) → [Self-Extension](03-extension.md).
- **To drive it from a domain layer:** [The Descriptor](01-descriptor.md) → [The Engine](06-engine.md).
- **To judge it:** [Appendix B](appendices/B-design-rationale.md) → [Appendix E](appendices/E-formal-proofs.md) → [Appendix A](appendices/A-mathematical-background.md).
