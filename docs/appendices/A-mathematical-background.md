# Appendix A — Mathematical Background: Frames as Physics

*The coherence argument: what recurs when the machine is read as a way of analyzing the world.*

---

## 11. Frames as physics (a reading)

**Why this section exists.** It is not ornament, and it is not a claim to have built physics. It is a **coherence argument** — evidence that the Plexus is a sound way to *analyze the world*, not merely a classifier of one toy dataset. The argument runs by construction: nothing physical was ever put into the machine. It is a pure structural router — symmetry, a chain of invariants, an exact residual — with no equations of motion, no action, no Planck constant, no metric. Yet when a stream of grids is read as **frames of an object over time**, the scaffolding of physical law falls out *unforced*, piece for piece: conserved charges, entropy as the log of a fiber, least action as the intersection of slices, the sum over paths, the classical–quantum fold, the renormalization flow. A framework earns the right to be called a coherent ontology when **the side-effects we already expect from how we observe the world re-appear as theorems of its own structure** — when you recognize the world in it without having inserted the world by hand. That recurrence is the proof on offer here: the same slice-and-find-the-constant logic that routes a shape under D4 is the logic by which we read conservation, causality, and scale out of nature. The correspondences below are each one instance of that recognition.

This remains an **interpretive lens, not implemented mechanism** (the caveat at the end says exactly what the code does and doesn't do) — but that honesty is *what makes the argument carry weight*: the parallels are exact enough to state plainly precisely because they were not engineered to fit.

---

### Reading map: the basis, and which example rides which axis

The examples below span a small **basis** — about six independent axes — but the physics ones cluster heavily on the first. This map states the basis so you can read §11 **either way**: down a row for the *coherence argument* (the same structure recurring), or **one example per row** for the *fastest collapse* (the spanning set — the minimum that triangulates every axis once).

| # | Axis (the basis vector) | What it fixes | Examples in §11 |
|---|---|---|---|
| 1 | **Slice / conservation** | fix a symmetry's coordinates; what stays constant *is* the conserved quantity (the descriptor) | *The correspondence · Conservation · Time-as-cursor · Law-as-compression · Least action · Sum over paths · Classical↔quantum · Drive + gate · Relativity · Light cone* |
| 2 | **Fiber / entropy** | the un-fixed remainder — `log\|fiber\|` — whose floor is the residual / twin | *Entropy = log of the fiber · Playing a position* |
| 3 | **Volume = determinant** | the *size* of the collapse, measured as a determinant | *The thread is the determinant* |
| 4 | **Decision without the volume** | act under a utility / stopping rule when the fiber can't be computed | *Utility and strategy · Playing a position* |
| 5 | **Symmetry-breaking** | decidability lives in the *break* (rectangle / D2, depth↔breadth, dimorphism) | *Symmetry breaking: the rectangle* |
| 6 | **Change of scale** | the fold *across* scales (renormalization / inflation) | *Change of scale* |

Row 1 is sampled **ten ways on purpose** — the recurrence across physics is the coherence argument, and a reader who wants to be *convinced* should read across it. Rows 3, 5, and 6 are sampled **once each**, rows 2 and 4 twice (*Playing a position* triangulates both from the contest side) — still below the coverage of row 1, so a reader who wants to *grasp the structure fastest* should read **one example from each row** and treat the rest of row 1 as confirmation, not new content. The rows are the axes; the map is the basis; the imbalance is deliberate breadth on axis 1 and minimal coverage elsewhere.

---

### The correspondence

| Plexus | physics |
|---|---|
| invariant part of the descriptor (`Shape`: mass, connectivity, symmetry) | the **macrostate** / conserved quantities (*what it is*) |
| cropped **pose** (translation, rotation) | the configuration coordinates where **momentum / motion** live (*where it is*) |
| a **slice** (fix the coordinates naming a symmetry) | a conservation law; the slice you cut **is** the law (Noether) |
| constant along the slice | a **conserved quantity** (constant of motion) |
| the **law** = a state→next-state rule independent of *when* | **time-translation invariance** = energy conservation |
| descriptor-fiber `D⁻¹(key)` (all grids with one macrostate) | the **microstates**; `log\|fiber\|` is the **entropy** |
| the **label** (`1`/`0`) read at each step | the **alive / halt** bit — survival under the dynamics |
| the irreducible **symmetry-twins** (exact-memory exceptions) | the built-in `0`s — the **death points** no symmetry can argue away |
| the context **trie** | the Turing tape **quotiented by the symmetry group** — content-addressed, not positional |

---

### Conservation = a constant along a slice

A conserved quantity is a **constant of motion**: cut a slice, walk the frames, and *whatever stays the same is conserved*. The slice you choose is the law you find — cut along translation → **momentum**; along rotation → **angular momentum**; along time with a fixed rule → **energy**; along the invariant → **mass / identity**. "The same" lives at two orders: a **scalar** charge (mass, energy) is flat in *value*; a **vector** charge (momentum `m·v`) is flat in *rate* — the pose marches at a constant step. A **bend** in the flat quantity localizes a **force**. Caveat: the default descriptor is translation-invariant, so it folds momentum away — to read momentum you keep the pose (the `[4] by grid` slice) and watch the placement advance uniformly.

---

### Time is the cursor, not a category

A *law of physics* is not a new category — it is the requirement that the state→next-state rule **not depend on when** (time-translation invariance), which *is* energy conservation. Time is the **playback cursor** you iterate the rule along, not an axis you route on. Make time a category and you get a flipbook, not laws.

---

### The law is a compression

A lawful trajectory needs only **(initial frame + rule)**; the rest regenerate by iterating — that's compression. The descriptor does **spatial** compression (crop the position → fold the orbit to one code), the law does **temporal** compression (one rule instead of every frame). Like a video codec: conserved quantities are the **compressible** skeleton (stored once), entropy is the **incompressible** residual, and a **force is a keyframe** — the surprise the predictor couldn't fold. A *lawful* universe is a *compressible* one; finding the laws is finding the best compression (Kolmogorov / minimum description length). The trie makes this operational ([§5](D-performance-notes.md), "path length = description length"): the lazy walk's resolution depth *is* the description length of a label's dependence — laws stop shallow, facts walk to the grid.

---

### Entropy = log of the fiber

The descriptor is the **macrostate**; its fiber `D⁻¹(key)` is the set of **microstates** that share it; so **entropy = `log\|fiber\|`** — and the possibility report `[7]` literally counts it. Conservation *enables* entropy rather than opposing it: a conserving (volume-preserving, Liouville) flow cannot shrink the cloud of states, so it can only spread and mix until it fills the surface the invariants allow — **maximum entropy = the largest fiber**. This finally supplies the **arrow**: conservation draws a reversible **rail**; entropy sets the **direction** along it (overwhelmingly toward bigger fibers, simply because there are more of them). Conservation says *where you can go*; entropy says *which way you almost certainly will*.

---

### Least action = thread the intersection

Each conserved quantity confines the trajectory to its slice; conserve several and you are pinned to their **intersection** — a thin residual channel, and **time is motion along that channel**. Free motion conserves the most → the full intersection → a straight line, the **geodesic**. A force *looks* like broken conservation but for the **closed** system (object + field) everything is still conserved — the charge merely **flows between subsystems**. The precise selector is *extremize the action*; "most conserved" is the exact intuition for the free/closed case.

---

### The thread is the determinant

The "thin residual channel" of the previous section — the descriptor-fiber `D⁻¹(key)` a grid is pinned to once its conserved coordinates are fixed — is the **thread**, and `log|fiber|` is its entropy. That slogan can be made an **executable identity** rather than a metaphor: the volume of the thread *is* a determinant. `ThreadVolume` (`org.tervel.plexus.ops`) computes that volume **five ways** at each node, and `VolumeReport` (`[v]`) checks they agree:

- **`|fiber|`** — the **combinatorial** volume: how many `side×side` grids route here (`Fiber.count`). The thing this section calls `|fiber|`, whose log is the entropy.
- **`|orbit|`** — the **rigid orbit** of a representative member, in the bounding-box frame (translation quotiented out): the distinct images of the centred shape under the group.
- **`[G:Stab]`** — the **orbit-stabilizer index** `|G|/|Stab|`, read straight off the key's stabilizer. This is the **group-theoretic determinant** of the action — the index `[G:Stab]` is the multiplicative "volume" of the coset space, and `Shape.isotropy = |Stab|/|G|` ([§6](../06-search.md), [§9](../07-descriptors.md)) is its reciprocal.
- **`√det(J Jᵀ)`** — the **Gram determinant of the descriptor's Jacobian**: stack the discrete gradients of the invariant *components* (the unpacked `Mass` and `Connectivity` rows, never the packed `Signature` value — `Invariant.components()` is the seam that exposes them, since a finite difference of the `V·W + E` packing mixes the incommensurable cell- and edge-units) into a matrix `J`, and `√det(J Jᵀ)` is the discrete co-volume the coordinates carve out. This is the **coarea / change-of-variables** reading — the one that *literally* means "the thread is the determinant": the factor relating a count in the ambient frame to a measure along the thread is the Jacobian determinant of the slicing map `D`.
- **`det L`** — the **Matrix-Tree determinant**: the number of spanning trees of the cell-graph, `det` of its reduced Laplacian (`L = BBᵀ`). The graph-theoretic volume, and the rigorous form of the [§3](../03-invariants.md) `V − E` path/tree/loop dial — the cycle rank `b₁ = E − V + C` is the dimension of the thread's cycle space.

The **exact identity** the report verifies is the orbit-stabilizer theorem, `|orbit| = [G:Stab]`: measured in the bounding-box frame, the thread *is* the group determinant. The full-frame `|fiber|` sits alongside and is in general larger — by exactly the **translational multiplicity** (`|fiber|/|orbit|`, how many places the box fits in the grid), the residual entropy the rigid group cannot see. So `id = ok` certifies a context that is **one tight orbit** (the 3×3 T: `|orbit| = [G:Stab] = 4`, with `|fiber| = 8` exposing the factor-2 translation slack), while `MISMATCH` flags a context inflated by translation or a mix of shapes (the 5×5 path-fork context, whose canonical stabilizer is trivial purely because D4 acts about the grid centre, not the off-centre shape). The divergence is not smoothed over — it is the **empirical handle** on exactly where, and by how much, structure under-counts the fiber. This is the same move as the invariance contract ([§3](../03-invariants.md)): a relation that was prose becomes a checked number — `Invariant.invariantUnder` made the invariance executable; `ThreadVolume` makes *thread = determinant* executable.

---

### Sum over paths (Feynman)

Least action picks the *one* extremal trajectory; the path-integral keeps **all** of them and sums. The machine has the discrete version already: sweep the group, and each move is a **possible path**. The moves under which the input keeps its label are the **allowed paths** (it stays on its orbit); the rest are **forbidden** (the label dies). The set of allowed paths *is* the input's **stabilizer** — so the stabilizer is the bundle of histories that keep the input itself, and "summing over paths" is summing over that orbit. A judgement on the input is therefore a judgement on its stabilizer: a large stabilizer = many allowed paths = a long-lived history; the first forbidden move names the symmetry it lacks. The `[1] by stabilizer` query is exactly this readout — allowed vs forbidden paths, and the **history class** they sum over. Honest seam: the moves are *spatial* transforms, so these are symmetry paths, not the time-paths a true action integral sums (the time-drive is unbuilt, per the caveat).

---

### Utility and strategy: deciding without the volume

The sum-over-paths keeps *every* history and the descriptor-fiber `D⁻¹(key)` counts them — `log|fiber|` is the entropy, the volume of everything that happened (§*Entropy = log of the fiber*). Knowing everything means enumerating that volume (`Fiber`, `ThreadVolume`). But often you must **decide without computing the volume** — and that is a different, decision-theoretic layer, built in `org.tervel.plexus.utility`.

The seam is already present: a `ResidualResolver` ([§5](../02-core-model.md)) makes an implicit choice — it picks the label minimising error, the **argmax under a hidden 0/1 utility**. `Utility<T> extends ToDoubleFunction<T>` makes that valuation explicit and reusable (the same idiom as `Invariant extends ToIntFunction`): it answers *how good is this outcome?* where an invariant answers *what coordinate is it?*.

On top of a utility sits **optimal stopping** — the secretary / **Sultan's dowry** problem (`OptimalStop`): outcomes arrive one at a time, scored by the utility, and you must commit or discard on the spot with no recall. You cannot integrate the whole volume; you carry only a **running benchmark register and your position**. The classic solution is the **`1/e` rule** — observe (reject) the first `⌊n/e⌋`, then take the first to beat them — which lands on the global best with probability `→ 1/e ≈ 0.368`, a deliberately **volume-blind** rule.

The rule is factored into a **harness (`OptimalStop`) and a `Strategy`** — the explore-vs-commit personality dial. The shipped trio anchors the **safe↔risky axis with the dowry as the calibrated `NEUTRAL`**: `SAFE` (greedy, take the first — risk-averse pole, `P(best)=1/n`), `NEUTRAL` (`1/e`, maximises `P(best)`), `RISKY` (holdout, wait for the best — risk-seeking pole, `P(best)=1/n`). Neutral is not a compromise but the **fixed point of the two regrets** (settling-for-less vs ending-empty), which is why it is the unique optimum the poles fall away from (`OptimalStopTest` asserts exactly this: ≈0.38 at neutral, ≈0.05 at both poles). `Strategy.risk(factor)` scales riskiness relative to neutral, each doubling **halving the remaining distance to the pole it leans toward** — `risk(2)` ("twice as risky") sits at `(1+1/e)/2 ≈ 0.684`, halfway from the dowry to pure holdout.

Two honest seams. **(1)** This is **decision theory** — one agent against indifferent chance — built on the same expected-utility foundation as **game theory**, which is its multi-agent sibling (add a second chooser competing for the same stream and the threshold stops being a personality and becomes a strategy that must best-respond to the opponent's — a *stopping game*, unbuilt). **(2)** The `1/e` rule is blind because the abstract problem *cannot* see the remaining volume — but a Plexus **can** (`Fiber`, `ThreadVolume`), so it is uniquely equipped to run the **volume-aware** (full-information) rule instead: a declining threshold read off how much fiber remains, which `Strategy.commit(index, total, value, benchmark)` is general enough to host as a future sibling of the threshold family. The residual where even this runs out — two outcomes tied on every admissible measurement — is the **symmetry-twin** ([§10.4](B-design-rationale.md)): the choice no rule can make, left to the exact-exception store (the choice function supplied by fiat, the finite stand-in for an Axiom-of-Choice selection).

---

### Playing a position: the collapse accounting

The decision layer above chooses *when to commit*; this reading is about *what one move costs*, learned from playing positions rather than building them. Four laws, each a §11 axis worn in a contest:

- **Felt uncertainty is an entropy meter, not a verdict meter.** Mid-contest dread measures `log|fiber|` — how many ways the position could still go — not which way it goes. A **pure context** (the label effectively fixed by the matchup) feels calm or hopeless and *is*; a **collision context** (both labels live) feels like losing even while won. The feeling is well-calibrated for the wrong quantity: it reads the volume of the thread, never its direction. And the split names the seam exactly: in pure contexts the *structure* decides; in collisions structure is exhausted and the **residual resolver** — the pilot — decides. Trust the dread as a signal that your decisions now matter, not as a prediction of how they'll land.

- **Outcomes key on coverage, not drama.** Attention binds to the opponent's centerpiece — the most salient threat — but the label is set by the coverage table: which of your answers touch which of their threats. The one-question audit is *"can their big thing touch my answers to it?"* Salience is a property of the threat; coverage is a property of the *pair* — and only pairs route.

- **A move's measure is its net collapse.** Playing a piece collapses three things at once: the **resource** (spending the conserved charge breaks the affordability symmetry — at a full bank you are every plan you can afford, indistinguishably; the spend makes the expensive futures stop being yours, so the bank is *how undecided you are allowed to remain*), the **availability** (the piece is out of rotation; threats it answered are open until it returns), and the **information** (the opponent's map updates — a play is also a publication). Count options live before and after, yours and theirs: **net collapse = what they lost − what you lost.** A good move is not one that looks strong; it is one whose net collapse is positive. The same number explains the misplay: a strong piece spent on a weak target collapses you for nothing.

- **A wrong prior mis-prunes; the expensive failure is a trusted invariant outside its domain.** Time-to-solve a position is search volume ÷ pruning power — and the worst case is not shallow search but a usually-valid invariant ("defend what is yours") silently cutting the correct branch before evaluation. The search then *feels* complete while excluding the answer; the fix is prior-repair, not depth. This is the player-level form of the invariance contract ([§3](../03-invariants.md)): an invariant applied where it does not hold shatters the very orbit it usually protects — and the collision diagnostic is the institutional version of the friend who says *"that rule doesn't apply here."*

The open-set discipline that governs all four is [§8](../09-api.md)'s gather rule read as practice: the moves genuinely available are the ones no prior break distinguishes, **closed under composition** (two open moves whose combination breaks something were never both open), and unseen moves are *unknown*, not free.

---

### Symmetry breaking: the rectangle, and sexual dimorphism

The descriptor's grid is **square** because the square's symmetry (D4) makes the two axes *interchangeable* — the transpose is a symmetry, so "extent along x" and "extent along y" are the same kind of coordinate (Principle 6). Break that — make the grid a **rectangle** — and the transpose dies (D4 → D2): the two axes become **incommensurable**, two specialized dimensions that can no longer be folded into each other. A *vertical* box is then depth-first search (deep along one axis, narrow on the other); a *horizontal* box is breadth-first (its transpose partner); and choosing between them is choosing *which* dimension gets the deep search. The aspect ratio is how lopsided that commitment is — square = agnostic, a thin rectangle = "one axis is nearly everything." (Not only a reading: `Dihedral.d2()` implements exactly the D2 break, and the routing core handles rectangular domains — [§2](../04-symmetry.md).)

The break is now **measurable in the query vocabulary**. Under D4, `rot90`-conjugation swaps `mirrorH`↔`mirrorV`, so the two are one class and the canonical mask always spells it `mirrorH` — asking a square table for `mirrorV` returns nothing, a *dead spelling* ([§2](../04-symmetry.md)). Under D2 the only rotation is `rot180`, which **commutes** with both mirrors, so nothing folds and both stay separately askable: the rectangle's table lists `[identity, mirrorH]` and `[identity, mirrorV]` as distinct stabilizers. Breaking the transpose does not merely make the two axes different — it makes them **separately nameable**, and the count of things you can ask for goes up. The lost symmetry is paid back as vocabulary.

Read biologically, this symmetry-breaking **is sexual dimorphism.** Isogamy — gametes all one size — is the *square*: the two reproductive roles are interchangeable (and high-symmetry; many isogamous mating types form a regular n-gon, every role equivalent). Anisogamy — two gamete sizes evolving under disruptive selection — is the *rectangle*: the transpose breaks, and the population splits into a **depth specialist** (the egg: few, large, provisioned — *exploit*) and a **breadth specialist** (the sperm: many, small, motile — *explore*). The two sexes are the broken transpose; the egg/sperm split *is* the depth/breadth (DFS/BFS) split of the strategy section above, frozen into biology — which is why the dominant strategy under incommensurable axes is two specialists rather than one compromise. (Honest limit: this is rigorous at the **gamete** level — anisogamy genuinely is a depth/breadth bifurcation — and a just-so story above it. "Male minds vs female minds" collapses an orbit a single axis cannot carry — the [§10.4](B-design-rationale.md) trap, a scalar forced onto a whole behavioural fibre.)

The covering split (`org.tervel.plexus.symmetry.Crystallographic`) extends the same reading. *n* equal mating types are a regular n-gon, and only the **covering** orders {1, 2, 3, 4, 6} (Euler totient `φ(n) ≤ 2`) tile a periodic grid — these are exactly the symmetries Plexus can route over. So two anisogamous sexes is the symmetry-**broken**, low-symmetry case (the rectangle), while many equal mating types is the high-symmetry case (the n-gon) — counter-intuitively, *more* sexes means *more* symmetry, not a stranger one. The **non-covering** orders — pentagon and beyond, `φ(n) > 2` — cannot lie flat and periodic at all; their surplus symmetry escapes into **curvature** (a regular pentagon's 36° vertex gap closes into the dodecahedron), **aperiodicity** (Penrose), or a **higher dimension** (cut-and-project from `φ(n)`-D). That boundary is the one `Crystallographic` draws, and the aperiodic side is its standing TODO.

---

### Classical ↔ quantum (orbit vs representative)

Classical and quantum are then the **same bundle read two ways**. The bundle is the orbit — what the stabilizer names (above). **Classical** collapses it to its canonical representative (least action keeps the one stationary path); that collapse is exactly the **fold to the canonical stabilizer** ([§9](../07-descriptors.md)), and stationary phase (ħ→0) is what performs it. **Quantum** keeps the whole bundle and lets its elements **interfere** (sum over paths with phase `e^{iS/ħ}`). So the correspondence principle *is* the orbit-fold, and `isotropy = |Stab|/|G|` measures how much bundle collapses to a point. The Plexus is the **classical shadow**: it keeps the orbit as a *set* (a charge/partition), not a phased sum — no phase, no interference. Quantizing it is therefore one named, unbuilt move: **attach a phase to each orbit element and sum amplitudes** instead of partitioning allowed/forbidden. These are orthogonal knobs on the one frame-bundle: *conservation* fixes a coordinate, *relativity* chooses the foliation, *classical↔quantum* chooses whether to fold the bundle or keep it interfering — and only the last is already half-built (the machine does the fold).

> **TODO — phase-sum quantization, and the twin as a conjugate charge.** Attaching a phase `e^{iS/ħ}` to each orbit element and summing amplitudes is the one named, unbuilt move from the classical shadow to the quantum. It is also where the **symmetry-twin** ([§5](../02-core-model.md), [§10.4](B-design-rationale.md)) earns a physical reading: a twin shares every group invariant with its oppositely-labelled partner and differs only in a discrete sign the algebra cannot derive — a **Z₂ charge orthogonal to the geometry**, stored by hand (the exact-exception floor), i.e. the *classical, decohered residue of a particle–antiparticle pair*. Three honest seams bound the analogy: the operation relating Plexus twins is a **spatial** D4 transform, so it reads as parity/chirality (P/CP) before charge-conjugation (C, an *internal* op); the **majority/exception asymmetry** has no electron/positron counterpart (it is closer to matter/antimatter *abundance*); and antiparticles *originate* in the very phase this machine drops. So phasing the orbit is exactly the move that would promote the twin shadow toward a genuine e/p pair — with the **time-drive** (the **T** of CPT, the Stückelberg "antiparticle = particle backward in time") as the other missing ingredient.

---

### The machine: drive + gate

Split the Turing analogy in two. The **law is the drive** (the transition rule that advances the state); the **label is the gate** — `1` = advance / live, `0` = halt / die. So a configuration has a **lifespan**: how many steps the dynamics keep it landing in `1` before it falls onto a `0`. The **irreducible twins are the death points** — the `0`s the symmetry algebra provably can't smooth over. As it stands the machine is finite and read-only (always halts in fixed steps) — *not* Turing-complete; give it a **write** and **unbounded memory** and "how long does it live / will it halt" becomes genuinely undecidable.

---

### Relativity (the aspirational extension)

Push it further and **time becomes a foliation**, not an index: each observer is a way of slicing the frames into "now" surfaces, and observers **reshuffle** the order of *acausal* (spacelike) frames while agreeing on the *causal* (timelike) ones — the law fixes the timelike skeleton, simultaneity is the slack. Conservation survives the shuffle because **a constant is order-independent**: the same charge sits on every observer's slice (the field-theory statement that a conserved charge is the same on every Cauchy surface). What *transforms* between observers is the **split** — a boost mixes energy and momentum as components of one object — while the invariant the slice measures is shared.

---

#### The light cone from an ensemble of Plexuses

The foliation need not be postulated — it can be **derived from many routers**. Take an ensemble of Plexuses as observers, each fed the same frame-stream but **reordered by propagation delay** (each event reaches each observer at a different time). Each observer's ordering is then one slicing of the frames; the observer *is* its delay profile.

The physics is what they **agree on**. Some event-pairs get reordered between observers (whoever is closer sees one first); some pairs reorder for *no* observer. The orderings that survive **every** observer are the **causal partial order**, and its frontier — the boundary between "all observers agree before/after" and "observers disagree" — **is the light cone**. The cone is thus the *invariant of the reshuffling*, not a separate postulate drawn on top. The Plexus **charges** (the invariant descriptor — mass, connectivity, the symmetry verdict) are frame-*content*, not frame-*order*, so every shuffled observer agrees on them (the same charge on every "now"); only the **pose** is observer-dependent — the part a boost mixes.

The one ingredient this does **not** give for free: a *per-observer* delay yields only a causal partial order, which could still be **Galilean** (a universal "now" recoverable by correcting for distance). To get the **Lorentz** cone — the same opening everywhere, time dilation, boosts — the propagation speed must be **one invariant for all observers** (`c`). That single constraint is what makes the distance-correction unshareable (the irreducible disagreement *is* the cone) and forces the transformations between observers to be **boosts that mix the space and time axes** rather than mere relabellings of arrival time. So: the ensemble gives the causal **skeleton** for free; making `c` an invariant of the ensemble (not a per-Plexus parameter) is the postulate that promotes it to a **metric cone** and pins the boost group.

---

### Change of scale (renormalization) — the half-built knob

Alongside *conservation* (fix a coordinate) and *relativity* (choose the foliation), there is a third knob on the same frame-bundle: **change of scale**. Coarse-graining a grid — block-decimation, `side → side/k` — is the renormalization-group flow, and it is the **same fold** [§9](../07-descriptors.md) already runs, applied per block instead of per strand: each `k×k` block collapses to one coarse cell, and the **microstates it discards** are `log|orbit|` bits, the degrees of freedom integrated out. `Scale` (`org.tervel.plexus.ops`) is that operation, the explicit sibling of `Decompose`: same orbit collapse, different unit. The block fold *is* the classical limit (§11, fold-to-representative); the unbuilt **quantum** move would keep those microstates with a phase and sum amplitudes — `Scale` only *measures* the discarded count (`integratedEntropy`), it does not sum it.

The decimation needs `k | side`, which is where the frame's own arithmetic enters: a **prime side has no such `k`** (only the trivial `1` and `side`), so a prime frame is **scale-rigid** — the renormalization knob is blocked, there is no non-trivial coarse-graining at all. `PrimeFrame` (`org.tervel.plexus.ops`, the `[p] prime` menu reading) makes this executable: it reads the **translational** action the router quotients away (the cyclic `Z_n`, whose subgroup lattice is the divisor lattice of `n`) and verifies, by the orbit law, that *every non-constant orbit has size `n` iff `n` is prime* — the rigidity that also forces `2^n ≡ 2 (mod n)` (Fermat, as a consequence of the indecomposable frame; the report flags the congruence a necessary-only proxy and the orbit test the exact form). It characterizes one frame's primality structurally; the **distribution** of primes — how the scale-rigid sides thin out — stays the analytic residual the symmetry points at but does not derive ([§5](../02-core-model.md)).

The decimation must **commute with the group** (`k | side`, a centre-symmetric tiling, a D4-invariant per-block readout) — the analogue of a regulator that preserves the symmetry. This is where the **quantum/classical conflict** is meant to surface, via renormalizability: feed the coarse grid back to the **collision diagnostic** ([§3](../03-invariants.md)) across two scales and watch whether the invariant chain **closes** (a finite fixed point — renormalizable: the classical fold and the quantum fiber reconcile) or whether each coarse step emits a **new blueprint** (unbounded counterterms — non-renormalizable: the classical fold has no finite quantum completion, and the diagnostic names the very primitive the fold failed to keep). The **two-scale closure loop** that runs this flow is built, and asserted rather than merely printed (`RenormalizationTest`): it takes the fine-scale `CollisionDiagnostic` reading, folds the dataset with `Scale`, retrains a fresh `Plexus` at `side/k`, and reads the coarse diagnostic — then issues a three-way verdict framed in Plexus's own **exception** terms (the diagnostic exposes a structured `analyze` result for this). The quantities it compares are the **blueprint** (the set of `Primitive` names the diagnostic emits for *separable* collisions — the **reducible** counterterms a richer chain could absorb) and the **floor** (the count of *provable-twin* collisions — the **irreducible** exceptions that land permanently in the exact store):

- **renormalizable closure** — the fold preserved the floor and demanded no new counterterm (a finite fixed-point chain across scales);
- **soft non-renormalizability** — the floor held but the coarse scale demands a *new* reducible counterterm (renormalizable only with an enlarged chain);
- **hard shattering** — the fold turned a separable distinction into a **twin**, minting *new irreducible exceptions* and raising the floor — information no invariant can recover.

The shipped experiment is the **path-vs-fork** collider scaled to 9×9 (a T-tetromino fork and an L-tetromino path sharing `(V=4, E=3)` and a trivial canonical stabilizer, so they collide in one separable context with `blueprint = {max-degree}`, floor 0). Under the `k = 3` fold each tetromino crushes to one or two coarse cells; the diagnostic reports **hard shattering** — the path/fork distinction collapses onto identical coarse canonical shapes, manufacturing irreducible twins. (A `side ≥ 4` grid is required at all: the 3×3/5×5 reference sets are degenerate for `k | side`.)

Still unbuilt: the **phase-sum** that would complete the quantum side. The loop measures the discarded `integratedEntropy` but does not sum amplitudes over it, so the conflict surfaces as the floor rising rather than as interference — the exact place the unbuilt phase-sum (§11, *Classical ↔ quantum* TODO) would attach.

---

### What the symmetry holds — and the one thing it can't

The intuitions above (and the collapse / opposite-orthogonal-null vocabulary they ride on) reconcile with the implementation at **three different costs**, and the cost is exactly the concept's distance from the symmetry machinery.

**Held already — the code does it, only the bridge was unwritten:**

- **Re-symmetrization → undecidable twin.** Breaking the transpose (`Dihedral.d2()`, the rectangle) is what *lets* a structure collapse to an answer; restoring it (`d4()`, the square) makes the two axes transpose-equivalent — a twin — and undecidable. Decidability is a function of the *broken* symmetry; the magnitude of the break is the confidence of the collapse.
- **Pose vs. invariant (mood-invariance).** `Shape` is the pose, the descriptor is the invariant, and `Invariant.invariantUnder` is *why* the structural read is mood-invariant: a non-invariant perturbation is, by contract, discarded. Reading structure is robust to the crowd's mood precisely because the contract throws the mood away.
- **The residual as an unhoused scalar ("hype").** `ExactExceptions` stores what no group-invariant can resolve. Any external scalar with no structural home is a residual of the same kind — the price gap between two structural twins (value not *in* the object) is exactly this, read outward.
- **The expert is the diagnostic.** `CollisionDiagnostic` *is* the expert: it relocates a collision from underdetermination (a scalar with many candidate carriers) down to the irreducible twin floor, and names the separating axis when one exists.

**Held only at a different order — covering-but-unbuilt:** the **triad** — three-as-minimal-closure (one = null, two = unstable duality, three = the first *closed* structure; Trinity / Trimurti / Mandate of Heaven) — is a **D3** (ternary) symmetry, *not* D2. It is covering (`Crystallographic.covers(3)`) but unbuilt; the intuition maps, the *runnable* form waits on the triangular lattice. The middle term (preserver / Son / Ruler) is the order-3 fixed locus — remove it and the closure degenerates to a line.

**Not held by any group — the framework's own residual:** the **null** (indifference; the collapse to zero). No group element represents it: a symmetry group is *invertible bijections*, and nothing maps to nothing. The null is the **universal fixed point** — fixed by every transform precisely because it is the *absence* of the object the group acts on — so it lies outside every orbit. The code contains it only as degenerate cases (the empty grid, an unseen key defaulting to reject, the trivial stabilizer); it can be **labelled** (here, and at `SymmetryGroup`) but never **enacted**. This is the [§10.4](B-design-rationale.md) twin floor turned on the framework itself — the one concept the symmetry can point at but never hold, and fittingly it is the concept *of* non-collapse: the opposite of love is not its negation (hate, a flip) but its *absence* (indifference, the zero).

So the reconciliation grades its own documentation debt: the mappable intuitions cost a **sentence**, the different-order one costs a **lattice** (D3), and the unmappable one costs a **label** — each price set by how far the idea sits from the group.

---

### Caveat — what the code actually does

This is a reading. The implementation has the **spatial group (D4)** and a plain **frame index** — no metric, no light cone, no foliation, no entropy dynamics, no write-back, and no ensemble of observers. Realising the relativistic extension would mean building the ensemble construction above: many routers fed delay-reordered frames, an **invariant `c`** shared across them (the postulate that turns the causal partial order into a Minkowski cone), and swapping D4 for a group that includes **boosts** (mixing the space and time axes). The encouraging part is that *nothing in the slice-and-find-the-constant logic changes* — the causal structure is just "what is fixed under the boosted group," the same stabilizer/orbit machinery that routes a shape under D4; you would only be cutting slices through a larger group.
