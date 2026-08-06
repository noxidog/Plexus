# Search: the Descriptor Run Backwards

*The symmetry sweep and the descriptor-fiber enumeration.*

---

## 6. The Searcher: the descriptor run backwards

The descriptor is a map `D : grids → keys`. Classification runs it forward; the **`Searcher`** runs the symmetry direction backward. Given an `input` grid and a `target`, it sweeps the group and yields the transforms whose image the Plexus classifies as `target`:

```java
group.transforms().stream().filter(t -> plexus.verdict(t.apply(input)) == target)
```

The group is the search space. It reads out an input's symmetry structure directly:

- `POSITIVE` on an exception → its **broken** symmetries (transforms that turn it into a genuine, positively-classified grid),
- `NEGATIVE` → its **surviving** symmetries (under which it stays the exception),
- `UNSEEN` → the transforms that carry it **off the trained map** entirely.

Matching on `verdict` rather than `score` is what makes those three a genuine **partition** of the group ([§5](02-core-model.md)): under `score` the reject and the negative class share the value `0`, so a `target = 0` sweep would count never-explored transforms among the surviving symmetries. For a flipped orbit-member it returns the broken transforms (those that map it onto a genuine positive) against the surviving ones — the broken/surviving partition, in the object's own frame, with the unexplored remainder named rather than absorbed.

**Why it is ever non-trivial.** If the chain is orbit-constant ([§3](03-invariants.md)) and no exception is involved, every image routes to the input's own context and the sweep returns all of the group or none of it. It resolves to something in between exactly where orbit-constancy is *deliberately* broken — the exception store (which matches raw grids, so an image can be a memorised exception when the input is not) and the radial-only `Position`. The Searcher is therefore the instrument that measures **where the quotient leaks**: it reads out the pose-dependence routing normally folds away. (`UNSEEN` is all-or-nothing for a legal chain, since unseen-ness is a property of the key and the key is orbit-constant.)

---

### The fibration: classify and generate

`D : grids → keys` is a fibration. Classification runs it forward; **generation samples its fibers** `D⁻¹(key)`. A **context** *is* a generator of its members — defined by the constraint conjunction (stabilizer ∩ mass ∩ connectivity ∩ position), not by a stored list, so the fiber includes members never trained on. The label splits the fiber: majority members vs the exception twins. ("Context" is the view a leaf projects; "node" is reserved for the trie itself.)

---

### Enumerating the descriptor-fiber in the bounding-box frame (`Fiber`)

`new Fiber(key, side).apply(plexus)` inverts the descriptor: every `side×side` grid that routes to this exact node — `D(g) == key`. It unpacks the mass from the key's `Signature` coordinate (`Signature.massOf`; no example grid needed, so it works for pure contexts), enumerates in the bounding-box frame, and keeps only candidates whose descriptor equals the key. The fiber factors:

```
D⁻¹(key)  =  match-descriptor  ∘  translate-place  ∘  bbox-patterns(key)
```

> The **shape-fiber** (`Shape.of⁻¹`, a shape's geometric instances) is the *conceptual* sibling below; only the **descriptor-fiber** is wired as an operation (`Fiber`). `Shape.of` is used forward — for a shape's identity and isotropy. A shape-fiber enumeration would be the geometric union over positions; build it only if an "all instances of this shape, any position" operation is actually needed.

- **bbox-patterns** — tight `h×w` patterns of the key's mass that touch all four borders (box exactly `h×w`, no empty margin). The box isn't a key column, so it **tries every box shape** `bh×bw` and lets the descriptor filter cut the wrong ones; the tight-pattern count for a fixed mass is small and grid-size-independent.
- **translate-place** — slide each tight pattern over the grid's `(side-h+1)·(side-w+1)` box positions. The *only* factor grid size enters — cheap and multiplicative.
- **match-descriptor** — keep a placed candidate iff `D(candidate) == key`, enforcing the residual stabilizer / connectivity / position constraints. Tightness generates each member once.

---

### A word on "generate"

*Generate* is the name of a **direction** (the classify/generate duality of principle 5), not of the operation. What `Fiber` actually does is **enumerate / invert a fiber** — exact, exhaustive, deterministic constraint solving, not sampling. So: "generate" for the duality; "enumerate the fiber" / "invert the fibration" for the mechanism. And since there are two fibrations, name which one — the **shape-fiber** (`Shape.of⁻¹`, conceptual) or the **descriptor-fiber** (`D⁻¹`, the wired `Fiber`).
