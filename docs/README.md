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

## Reading orders

- **To use it:** [Design Overview](design.md) → [The Interpreter](05-cli.md) → `../data/README.md`.
- **To understand the key:** [The Descriptor](01-descriptor.md) → [The Trie](02-trie.md).
- **To query and generate:** [The Trie](02-trie.md) → [Slices](04-slices.md).
- **To see how it grows:** [The Trie](02-trie.md) → [Self-Extension](03-extension.md).
- **To drive it from a domain layer:** [The Descriptor](01-descriptor.md) → [The Engine](06-engine.md).
