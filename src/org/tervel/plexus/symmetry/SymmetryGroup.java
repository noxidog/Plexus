package org.tervel.plexus.symmetry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An ordered group of {@link Transform}s over square grids. Everything else is built from
 * <b>stabilizer</b> — which transforms fix a grid — in three forms: the raw self-symmetry, its
 * rotation-canonical form (the routing descriptor), and the "is any rotation a fix?" test (the radial
 * verdict).
 *
 * <p><b>Ordering is a contract.</b> {@link #transforms()} is ordered {@code identity, rotations…,
 * reflections…} and the index of a transform <em>is</em> its bit position in every mask this interface
 * returns ({@link #stabilizer}, {@link #canonicalStabilizer}, {@link #describe}). Consumers (the routing
 * descriptor, the broken/surviving sweep) rely on that alignment, so a derived subgroup
 * ({@link #rotations()}) must preserve relative order.
 *
 * <p><b>Scope: covering groups only.</b> An implementation may exist only for a <em>covering</em>
 * (crystallographic) symmetry order — {@code n ∈ {1, 2, 3, 4, 6}}, the orders whose regular polygon tiles
 * the plane. {@link Dihedral#d4()} (square) and {@link Dihedral#d2()} (rectangle — the transpose-free
 * subgroup) are the realised ones; triangle (D3) and hexagon (D6) are covering-but-unbuilt. Non-covering
 * orders (pentagon and beyond) are aperiodic and provably cannot be a finite group of this form — see
 * {@link Crystallographic} for the split, the {@code φ(n) ≤ 2} criterion, and why Penrose covers fall
 * outside this interface.
 *
 * <p><b>No element represents the null.</b> A group is invertible — every transform has an inverse, so
 * <em>nothing maps to nothing</em>. The empty / zero configuration is the <b>universal fixed point</b>: fixed
 * by every transform precisely because it is the <em>absence</em> of the object the group acts on, and so it
 * lies outside every orbit. It surfaces here only as degenerate cases (the empty grid; an unseen key
 * defaulting to reject in {@link org.tervel.plexus.Plexus#score}; the trivial stabilizer) — it can be
 * <em>labelled</em> but never <em>enacted</em>. This is the framework's own residual: the one thing the
 * symmetry can point at but never hold (PLEXUS_DESIGN §11, <i>What the symmetry holds</i>).
 */
public interface SymmetryGroup {

    /** The transforms, in canonical order; index == bit position in every mask here. */
    List<Transform> transforms();

    /** The rotation subgroup (the transforms with {@link Transform#isRotation()}), order preserved. */
    SymmetryGroup rotations();

    /**
     * The self-symmetry of {@code g}: a bitmask over {@link #transforms()} of those that fix it. This is
     * the routing descriptor's symmetry component and a context's in-key symmetry set. The {@code int[]}-only
     * form assumes a square frame; the {@code (rows, cols)} form carries an explicit rectangular domain.
     */
    default int stabilizer(int[] g) {
        final var s = Transform.side(g);
        return stabilizer(g, s, s);
    }

    /** {@link #stabilizer(int[])} on an explicit {@code rows×cols} frame (a non-square domain). */
    default int stabilizer(int[] g, int rows, int cols) {
        final var ts = transforms();
        var mask = 0;
        for (var k = 0; k < ts.size(); k++) if (ts.get(k).fixes(g, rows, cols)) mask |= 1 << k;
        return mask;
    }

    /** The minimal non-trivial rotation — the generator of the rotation subgroup (rot90 on a square, rot180 on a rectangle). */
    default Transform minimalRotation() { return rotations().transforms().get(1); }

    /**
     * The rotation-canonical self-symmetry of {@code g}: the minimum {@link #stabilizer} over the
     * grid's rotation orbit. Stepping by the {@link #minimalRotation} folds a shape and its rotated
     * copies onto one key, so orientation drops out of the descriptor.
     */
    default int canonicalStabilizer(int[] g) {
        final var s = Transform.side(g);
        return canonicalStabilizer(g, s, s);
    }

    /** {@link #canonicalStabilizer(int[])} on an explicit {@code rows×cols} frame (a non-square domain). */
    default int canonicalStabilizer(int[] g, int rows, int cols) {
        final var step = minimalRotation();
        final var turns = rotations().transforms().size();
        var canonical = Integer.MAX_VALUE;
        var rot = g;
        for (var i = 0; i < turns; i++) {
            canonical = Math.min(canonical, stabilizer(rot, rows, cols));
            rot = step.apply(rot, rows, cols);                          // rotation keeps rows×cols (rot180 / rot90 on square)
        }
        return canonical;
    }

    /** Whether any non-trivial rotation leaves {@code g} unchanged — i.e. {@code g} is rotationally round. */
    default boolean rotationFixes(int[] g) {
        final var s = Transform.side(g);
        return rotationFixes(g, s, s);
    }

    /** {@link #rotationFixes(int[])} on an explicit {@code rows×cols} frame (a non-square domain). */
    default boolean rotationFixes(int[] g, int rows, int cols) {
        final var step = minimalRotation();
        final var turns = rotations().transforms().size();
        var rot = step.apply(g, rows, cols);
        for (var i = 1; i < turns; i++) {
            if (Arrays.equals(rot, g)) return true;
            rot = step.apply(rot, rows, cols);
        }
        return false;
    }

    // ---- Multiplication: the group computing with itself ------------------------------------------

    /**
     * The <b>multiplication table</b>: {@code table[a*n + b]} is the index of {@code a∘b} (apply {@code b},
     * then {@code a}). This is the group operation the rest of the interface previously did without — every
     * other method here <em>acts</em> on grids, applying transforms one at a time; multiplication lets the
     * group <em>compute with itself</em>, answering "which single transform is that?" with no grid present.
     *
     * <p>No new geometry is needed, because {@link Transform#src} already <b>is</b> the permutation. From
     * the contract {@code out[i] = in[src(i)]}, composing gives {@code src_{a∘b}[i] = src_b[src_a[i]]}, so
     * the table is read straight off the existing primitive on a 3×3 probe frame — the smallest square on
     * which all eight dihedral elements act distinguishably. The frame is a scratch pad only: the group
     * structure is frame-independent (the rectangular D2 subgroup composes identically on a square, which
     * is why it may borrow the same probe).
     */
    default int[] multiplicationTable() {
        final var k = 3;                     // smallest square frame on which D4 acts distinguishably
        final var ts = transforms();
        final var n = ts.size();
        final var perms = new int[n][];
        for (var i = 0; i < n; i++) perms[i] = permutation(ts.get(i), k);
        final var table = new int[n * n];
        for (var a = 0; a < n; a++)
            for (var b = 0; b < n; b++) {
                final var comp = new int[k * k];
                for (var i = 0; i < comp.length; i++) comp[i] = perms[b][perms[a][i]];
                table[a * n + b] = match(perms, comp);
            }
        return table;
    }

    /** Index of {@code a∘b} — apply {@code b}, then {@code a}. */
    default int compose(int a, int b) { return multiplicationTable()[a * transforms().size() + b]; }

    /** Index of the identity — the transform that composes with every other to leave it unchanged. */
    default int identityIndex() { return identityOf(multiplicationTable(), transforms().size()); }

    /** Index of {@code t}'s inverse: the {@code u} with {@code t∘u == identity}. */
    default int inverse(int t) {
        final var n = transforms().size();
        return inverseOf(multiplicationTable(), n, t);
    }

    /**
     * The <b>subgroup generated by</b> {@code mask} — the smallest closed set containing it. A stabilizer is
     * always a subgroup (the transforms fixing a grid are closed under composition), so requiring a set of
     * symmetries implicitly requires everything they generate: keep {@code mirrorH} and {@code mirrorV} and
     * you have necessarily kept {@code rot180 = mirrorH ∘ mirrorV}. Closure makes that implication explicit
     * rather than leaving a caller to discover it as an empty result.
     */
    default int closure(int mask) {
        final var n = transforms().size();
        final var table = multiplicationTable();
        var out = mask;
        for (var grew = true; grew; ) {
            grew = false;
            for (var a = 0; a < n; a++) {
                if ((out & (1 << a)) == 0) continue;
                for (var b = 0; b < n; b++) {
                    if ((out & (1 << b)) == 0) continue;
                    final var bit = 1 << table[a * n + b];
                    if ((out & bit) == 0) { out |= bit; grew = true; }
                }
            }
        }
        return out;
    }

    /**
     * Whether {@code mask} is a genuine subgroup — i.e. some shape could actually have it as a stabilizer.
     * A non-closed mask is not merely unobserved, it is <b>impossible</b>, and the two are worth telling
     * apart wherever an empty answer would otherwise be reported for both.
     */
    default boolean isSubgroup(int mask) { return mask != 0 && closure(mask) == mask; }

    /**
     * The <b>rotation-canonical spelling</b> of a mask: the minimum over its rotation-conjugates
     * {@code r∘t∘r⁻¹}. This is the mask-level twin of {@link #canonicalStabilizer}, which folds a grid's
     * rotation orbit; here the same fold is applied to the symmetry <em>names</em> alone, with no grid.
     *
     * <p>It is what makes a "dead spelling" detectable. Under D4, {@code rot90}-conjugation swaps
     * {@code mirrorH}↔{@code mirrorV}, so {@code {identity, mirrorV}} canonicalizes to
     * {@code {identity, mirrorH}} and is never the spelling the table stores — a real, achievable symmetry
     * set whose <em>name</em> canonicalization never uses. Under D2 the only rotation is {@code rot180},
     * which commutes with both mirrors, nothing folds, and both spellings stay live (§11, "the rectangle":
     * breaking the transpose is exactly what makes the two axes separately nameable).
     */
    default int canonicalMask(int mask) {
        final var ts = transforms();
        final var n = ts.size();
        final var table = multiplicationTable();
        var best = mask;
        for (var r = 0; r < n; r++) {
            if (!ts.get(r).isRotation()) continue;
            final var rInv = inverseOf(table, n, r);
            var conj = 0;
            for (var t = 0; t < n; t++)
                if ((mask & (1 << t)) != 0) conj |= 1 << table[table[r * n + t] * n + rInv];
            best = Math.min(best, conj);
        }
        return best;
    }

    /** A transform's source permutation on a {@code k×k} frame — {@link Transform#src} read as an array. */
    private static int[] permutation(Transform t, int k) {
        final var p = new int[k * k];
        for (var r = 0; r < k; r++)
            for (var c = 0; c < k; c++) p[r * k + c] = t.src(r, c, k, k);
        return p;
    }

    private static int match(int[][] perms, int[] target) {
        for (var i = 0; i < perms.length; i++) if (Arrays.equals(perms[i], target)) return i;
        throw new IllegalStateException("transform set is not closed under composition — not a group");
    }

    private static int identityOf(int[] table, int n) {
        for (var e = 0; e < n; e++) {
            var unit = true;
            for (var a = 0; a < n && unit; a++) if (table[e * n + a] != a) unit = false;
            if (unit) return e;
        }
        throw new IllegalStateException("transform set has no identity — not a group");
    }

    private static int inverseOf(int[] table, int n, int t) {
        final var e = identityOf(table, n);
        for (var u = 0; u < n; u++) if (table[t * n + u] == e) return u;
        throw new IllegalStateException("transform " + t + " has no inverse — not a group");
    }

    /** Names of the transforms set in {@code mask}, e.g. {@code "identity, diagMain"}; {@code "(none)"} if empty. */
    default String describe(int mask) {
        final var ts = transforms();
        final var present = new ArrayList<String>();
        for (var k = 0; k < ts.size(); k++) if ((mask & (1 << k)) != 0) present.add(ts.get(k).label());
        return present.isEmpty() ? "(none)" : String.join(", ", present);
    }
}
