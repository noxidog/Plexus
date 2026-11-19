# Appendix C — Rejected Designs

*Alternatives considered and retired, with the measurement or argument that retired them.*

---

<a id="deduce-dont-store"></a>

## Deduce, don’t store — the rejected stabilizer-per-trie-level expansion

**Deduce, don't store.** The first two rows are pure group facts: they never read the context table, never touch the data, and are computable from the requested mask alone. This retires a tempting alternative — expanding the stabilizer into one trie level per symmetry bit ([§5](../01-descriptor.md)) so that impossible branches could not exist and dead branches would visibly never be taken. That would have *materialized a derivation*: seven levels of structure caching what four lines of algebra compute on demand. The general principle, and the reason multiplication belongs in the group rather than in the index: **if a distinction follows from the group, derive it at read time; only store what the data decides.**

---

<a id="backoff"></a>

## Backoff — a measured negative result

**Backoff — a measured negative result.** The pitch: on an unseen key, answer from the deepest seen ancestor's majority instead of rejecting — coverage degrading rung by rung. Measured (hold out every example of one key, refit, query): systematically **wrong** on the reference datasets, worse than blind reject. The reason is minimality itself ([§3](../01-descriptor.md)): an invariant enters the chain only when the collision diagnostic proves it flips labels *within* a prefix bucket — so the deepest rung is always load-bearing, and the prefix majority is anti-correlated exactly where the full key matters. **A minimal chain has maximally treacherous prefixes.** Backoff pays only where labels are smooth along the chain, i.e. where the chain is over-provisioned; on data that earned every coordinate, the coarser context is precisely the wrong thing to trust.
