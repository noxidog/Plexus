# The Trie: the Chain as Paths

*The store that keeps keys as paths, and the self-extending configuration.*

---

## The chain as a trie (`org.tervel.plexus.trie`)

A hash table keyed on whole tuples stores only the **full** key; every coarser routing — "all shapes with this stabilizer", "…and this signature, ignoring the rest of the chain" — has to be recomputed by regrouping whenever a report wants it. `Trie` stores keys as **paths** instead — the track root, then the stabilizer edge (oriented track), then one edge per chain coordinate — so every **prefix is a live node** holding the merged `[neg, pos]` counts of its whole subtree. A depth-`k` node *is* the context you would have if the chain stopped at its `k`-th coordinate.

This **is** the router's store: `Plexus` holds a `Trie`, and `Context` is a view materialised from a leaf on demand rather than a bucket kept in a map. `contexts()` is therefore a **projection** — leaves walked in a deterministic order and reassembled into the `Key` whose path reaches them — which is what lets every report keep reading the same `Key`/`Context` surface while the storage underneath keeps paths. What follows is what the prefixes turned out to buy, and, measured honestly, what they don't.

**Training is the walk.** One example = walk down the symmetries, bump the answer at every node passed. The write side has no laziness — an example must land at its exact leaf — and that full walk touches every rung a read will ever consult, so every prefix count and every purity verdict stays fresh *by construction*. The leaf keeps its **members** (per exact grid, that grid's own `[neg, pos]`), and whether a member is an exception is **derived at read time** — member majority vs leaf majority, both live counts. This is what dissolves the sealing constraint (above), and it is why `fit` is one pass. The price is memory: in the `GRIDS` configuration every distinct training grid is held at its leaf, which is strictly more than an exception-only store would keep. The `DESCRIPTORS` configuration below settles that trade, because merging members by descriptor and growing an edge where a collision is separable can take the stored total **below** what memorising exceptions cost.

**Reading is the same walk, lazy.** Each coordinate (stabilizer, then chain) is computed only when its edge is needed, and the descent **stops at the first pure node** — one-sided counts mean no deeper coordinate can change the answer, so the remaining invariants are never evaluated. A pure node can hold no contradiction (an exception is a within-leaf contradiction, which would make every ancestor impure), so the early answer equals the full walk's answer on every seen key. The price is the **reject signal**: a pure-stop cannot tell a seen key from an unseen one that agrees up to the stop — the deep walk was never computing the answer, it was *verifying novelty*, and laziness spends exactly that.

**Twins force the full walk.** A twin contradiction at a leaf makes every ancestor impure — impurity propagates to the root — so pure-stop never fires on that spine: the twins' key-mates pay every rung *plus* the member lookup, and shapes sharing any prefix with the twins cannot stop until they step off it. The member lookup is really the chain's final coordinate: the **raw grid itself** — the identity invariant, infinite resolution, zero compression. Twins collide on every honest coordinate by proof ([§10.4](appendices/B-design-rationale.md)), so bit-for-bit identity is all that remains; the full walk is the twin theorem enforced at runtime.

**The prefixes are visible — the census.** `[t] trie` (`TrieReport`, [§8](09-api.md)) prints the store's own shape, which the context view cannot: contexts are the leaves, while the store also holds every prefix. Per rung it counts the nodes, how many are already label-pure, and how many are **redundant** — their parent was pure, so that rung's coordinate decided nothing for them and a radix merge would collapse them upward. Measured on `rect-bar-2x4` (asserted by `TrieTest.censusExposesRedundantRungs`):

```
  oriented track  (7 examples)
    depth  rung               nodes    pure  redundant
    0      (all oriented)         1       0          0
    1      stab                   3       2          0
    2      sig                    5       5          2
```

Two of the three stabilizer nodes are already pure, so the signature rung below them decided nothing — the compression the prefixes expose, read off rather than asserted.

---

## Self-extension: the second configuration

Everything above is `Trie` with an **empty extension vocabulary** and members kept as **grids**. Both are constructor knobs, not separate structures, because a self-describing trie whose axes happen to all be structural *is* the fixed-chain one. Turning them on changes two things, and they are one change seen from two sides.

**Descriptor memory instead of pixel memory.** Store each member's **coordinate vector** (the battery's outputs, plus the canonical `Shape` — the complete orbit invariant) instead of the grid, keyed so identical descriptors collapse: a twin leaf becomes one row with counts `[7,1]`, not eight grids. Everything the store is *for* survives, because the grid was never what was needed:

- **Splitting** reads a column, not a shape. Redistribution on extension is a table scan over kept measurements.
- **Twin detection becomes a local property**: rows differing in some column ⇒ separable, split on it; a single row with mixed counts ⇒ nothing in the battery separates them.
- **Regeneration** for the human is `Fiber` — the descriptor run backwards ([§6](06-search.md)). It is *not* unique (`entropy = log|fiber|`), but any legal invariant is orbit-constant, so it **cannot depend on which representative comes back**: the part destroyed (which pose) is exactly the part no future coordinate is permitted to use. Lossless in the only sense that matters — provided the vector pins the orbit (a canonical-form column), or the fiber spans several orbits and a representative may evaluate differently from the original.

**What is surrendered, precisely.** A member answers with its **own** recorded majority whenever its descriptor is distinct — so exact recall survives for everything the descriptor can tell apart, *including* collisions no battery column splits. It is lost only where two grids share every battery value **and** the same canonical orbit: the provable symmetry-twins ([§10.4](appendices/B-design-rationale.md)), which are exactly the set no invariant could ever separate. **The memory the design gives up is exactly the memory the theorem says is irreducible.** The price is that the battery is evaluated on every insert, not merely on collision — the grid is gone afterwards, so the measurements cannot be taken later.

**Growing the chain at insert instead of memorising.** The diagnostic runs as a batch post-pass emitting a blueprint for a human. But the moment a collision is *knowable* is the insert that causes it — the write walk is already computing every coordinate and is standing at the exact leaf, holding both colliding members. So `add` finishes by asking the leaf whether it can split: if some `Primitive` column separates the labels (information gain > 0), the leaf grows **one edge under itself only** and pushes its members into children (n-ary — as many children as distinct values, redistributed by *reading a column*, since the grids are gone); if nothing separates, the contradiction is genuinely irreducible and stays. Termination is structural: redistribution makes every child constant in the chosen column, so that column scores zero gain below and is never chosen twice on a path. The two halves of that decision live at different levels on purpose: *which column* is an injected **`ExtensionPolicy`** — the third seam, beside the symmetry group and the residual resolver; the shipped rule maximises information gain (`Information`, the same arithmetic the batch diagnostic scores the battery with) and declines at zero — while the twin verdict is the structure's own orbit test, run *before* any policy is consulted, because it is a theorem ([§10.4](appendices/B-design-rationale.md)) rather than a preference.

This forces the structure to be **self-describing** — each node carries the `Axis` its children are keyed by, rather than depth-indexing a global chain. That is the trie's real justification, and it is neither backoff nor laziness: **extension is local**, one node deepens and nothing else moves. A fixed-width key cannot do it even in principle, since adding a coordinate reshapes every key in the table at once.

**Measured** (`Trie.growing(...)`, declared chain a single `Signature`, extension vocabulary the `Primitive` battery; asserted by `TrieTest.documentedExtensionResults`):

| dataset | flat sealed | trie rows | irreducible | extensions |
|---|---|---|---|---|
| `conservation-t-orbit` | 0 | 1 | 0 | 0 |
| `entropy-fiber-gap` | 0 | 1 | 0 | 0 |
| `separable-path-fork` | 1 | 2 | **0** | **1** |
| `renormalization-path-fork-9x9` | **5** | 3 | **0** | **1** |
| `twin-t-flip` | 1 | 1 | **1** | 0 |
| `position-corner-dot` | 1 | 1 | **1** | 0 |

The headline is the 9×9 row: the fixed chain derives **five** exceptions; with extension turned on there are **none**, because the battery discovered `max-degree` at insert and grew one edge. Where a collision was separable, memory went to zero; where it was a provable twin, exactly one row survived. **Memory shrank to the twin floor and no further** — which is the design's claim, now measured rather than asserted.

Two consequences to accept: a primitive chosen from a thin collision is under-determined (vet it with the critic, [§3](03-invariants.md)), and the key stops being global — routing is path-dependent, decided edge by edge, so reports reading `Key.coords()` uniformly would have to read paths instead. This is the **Complexus** direction ([§11](appendices/A-mathematical-background.md)), with the trie as its substrate.

