# Appendix E — Formal Results

*The proofs the design leans on, and where each one is established.*

---

## The bounding box is a structural check, not an invariant

The shape's **bounding box is a direct readout of which symmetries can survive**: a non-square box (`long ≠ short`) is a *proof* that `rot90`, `rot270`, and both diagonal mirrors are not in the stabilizer — each transposes the box, and a thing with a different bounding box can't equal itself. This caps the stabilizer at a subgroup of `D2 = {e, rot180, mirrorH, mirrorV}`. It earns its keep as a **free radial pre-filter** — "radial" needs `rot90`, which needs a *square* box, so a non-square box is an instant proof of non-radial, and `isRadial` short-circuits on it before the centred-rotation check. The box is also a generation aid in `Fiber` (which derives it per candidate); it is never a routing column.

---

## Register of formal results

Every result the design leans on, and the file where it is stated and argued. The
statements live with the mechanism they justify; this register is the index.

| result | stated in |
|---|---|
| Symmetry-twins are irreducible by symmetry — two shapes sharing every symmetry quantity cannot be separated by any group-invariant | [Principles, §10.4](B-design-rationale.md) · [the residual boundary](../02-core-model.md) |
| Orbit–stabilizer as an exact identity, `\|orbit\| = [G:Stab]`, checked per context | [The thread is the determinant](A-mathematical-background.md) · [the volume report](../09-api.md) |
| Legal vocabulary grows monotonically up the subgroup lattice, from `\|orbit\| = \|G\| / \|Stab\|` | [Sectors: the two tracks generalized](../02-core-model.md) |
| The `V·W + E` signature packing is injective for `W = maxEdges + 1` | [The invariant chain](../03-invariants.md) |
| A non-square bounding box caps the stabilizer at a subgroup of D2 | this appendix, above |
| Composition needs no new geometry: `src_(a∘b)[i] = src_b[src_a[i]]` | [Multiplication: the group computing with itself](../04-symmetry.md) |
| The crystallographic restriction `φ(n) ≤ 2`, and `φ(n)` as the lift dimension | [TODO — covering domains](../10-implementation.md#todo-covering-domains) |
| `n` is prime iff every non-constant `Z_n` orbit has size `n`; Fermat `2^n ≡ 2 (mod n)` is necessary only | [the prime report](../09-api.md) |
| Pure-stop soundness: a pure node holds no contradiction, so the early answer equals the full walk's | [The chain as a trie](../05-trie.md) |
| Extension terminates structurally: redistribution makes each child constant in the chosen column | [Self-extension](../05-trie.md) |
| Sectors are unions of whole orbits, so a per-sector chain can never shatter a class | [Sectors: the two tracks generalized](../02-core-model.md) |
