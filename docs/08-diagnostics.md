# Diagnostics

*Automated invariant discovery, and the critic that vets a coordinate before admission.*

---

## Automated invariant discovery — the collision diagnostic

The chain grows by hand, but **which axis to add is now discovered automatically**. As a training post-pass, the **collision diagnostic** (`org.tervel.plexus.diagnostics`) examines every context with **non-zero entropy** (both labels present) and asks whether the collision is reducible:

- **Provable symmetry-twin** — the positive and negative grids share an orbit (equal canonical `Shape`). No group-invariant can separate orbit-mates ([§10.4](appendices/B-design-rationale.md)), so it is the irreducible floor and is left to the exact exception store.
- **Separable** — the positive and negative labels live in **disjoint orbits**, so *some* group-invariant differs across the split. The diagnostic sweeps a battery of universal topological **primitives** — `max-degree` (Δ), `components-b0` (b₀ = C), `cycle-rank-b1` (b₁ = E − V + C), `spectral-radius` (λ₁ of the adjacency matrix), all graph-isomorphism invariants and hence D4-legal — and isolates the one that **maximises information gain** across the colliding labels. It emits that primitive's **formal mathematical specification** as the exact blueprint for the next manual `Invariant`, and runs `Fiber` to confirm the primitive discriminates the whole descriptor-fiber, not just the trained sample.

The discovery is automated up to the invariance contract (only group-invariant primitives are in the battery); the **implementation** of the chosen axis as an `Invariant` is still manual design work. On the reference T-datasets the diagnostic reports every collision as an irreducible twin (the floor is already reached, confirming minimality); on a dataset where two equal-`(V,E)`, equal-stabilizer shapes of different branching collide, it emits the `max-degree` blueprint with an exact split.

---

## Vetting a coordinate before it enters the chain — the critic

The diagnostic asks *"is the chain deep enough?"* and proposes an axis. The **chain critic** (`ChainCritic`, menu `[c]`) asks the two questions that come **before** accepting one — aimed at the chain rather than at the data.

**Is each coordinate any good?** A coordinate can fail five ways, and only the first is a correctness bug:

| grade | test | meaning |
|---|---|---|
| `ILLEGAL` | `invariantUnder` run as a counterexample search over the group | not orbit-constant on the **oriented** track — orbit-mates route apart and the class shatters |
| `INERT` | distinct values over the applicable grids | one value: it never splits anything here |
| `REDUNDANT` | drop it, compare the induced partition | merges nothing — functionally dependent on the rest of the chain |
| `UNEARNED` | drop it, compare the **collision** count | it splits contexts but separates structures the labels never disagreed about — **minimality (above) made testable** |
| `DEGENERATE` | mean class size → 1 | an identity column: perfect information gain, zero generalisation — memorisation wearing a coordinate's clothes |

Together: *legal, useful, new, earned, not cheating.* `ORIENTING` is the fifth, non-fault verdict — a coordinate that **does** vary over an orbit but is radial-scoped, i.e. the documented `Position` exception; the critic reports the witness (`under rot90: 0 → 2`) rather than condemning it. The drop-tests are **positional, not by name**, so a chain carrying two same-named coordinates does not misreport each as redundant. Note the limit of one-at-a-time ablation: duplicate a coordinate and *both* copies grade `REDUNDANT` — each is individually droppable, though not both.

**At each collision, is extending even possible?** Three outcomes, and the first is what makes the tool worth trusting — it says when *not* to bother:

- **IRREDUCIBLE** — same orbit. The chain is not the problem; no orbit-constant invariant can ever separate orbit-mates ([§10.4](appendices/B-design-rationale.md)). Memory is forced. *Unless the context is `radial`*, which has no orientation to protect — there an orientation-scoped axis (`Position`) can still split it, and the critic says so. That is the one twin verdict with a way out.
- **SEPARABLE, battery has it** — name the winning column and its split (exact or partial; the diagnostic reports a "winner" even at zero gain, the critic distinguishes the cases).
- **SEPARABLE, battery is empty** — disjoint orbits guarantee a separating invariant exists, but nothing in the battery finds it. The critic renders both sides as **shapes** so the human has something to look at rather than two integers, and the new primitive is then vetted by the table above *before* admission rather than after.

Grades exercised by the reference datasets: `OK`, `INERT`, `REDUNDANT`, `ORIENTING`. `UNEARNED` and `DEGENERATE` are not reachable on them — those sets are orbit-narrow (even a canonical-shape hash takes only two values on the 9×9 path/fork data, and is graded `OK`, correctly).
