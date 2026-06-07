package org.tervel.plexus;

import org.tervel.plexus.invariants.Connectivity;
import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.invariants.Mass;
import org.tervel.plexus.invariants.Signature;
import org.tervel.plexus.ops.ThreadVolume;
import org.tervel.plexus.residual.ExactExceptions;
import org.tervel.plexus.symmetry.Dihedral;
import org.tervel.plexus.utility.OptimalStop;
import org.tervel.plexus.utility.Strategy;
import org.tervel.plexus.utility.Utility;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Demonstration of {@link Utility} + {@link OptimalStop}: first validates that the shipped default policy
 * really is the Sultan's-dowry solution (empirical marry-the-best rate → {@code 1/e}), then composes the
 * concept with the current objects — courting a trained Plexus's contexts in arrival order, scored by their
 * {@link ThreadVolume} (the thread's {@code |fiber|} volume), and committing by the {@code 1/e} rule.
 */
public final class SultanRunner {

    public static void main(String[] args) throws IOException {
        // 1. Slide the personality dial: over identical random arrival orders, the NEUTRAL (Sultan's dowry)
        //    strategy wins ~1/e while the SAFE and RISKY poles both collapse to ~1/n.
        final var n = 20;
        final var trials = 100_000;
        final Utility<Double> worth = Utility.of("dowry", Double::doubleValue);
        System.out.printf("Strategy dial  -  marry-the-best rate  [n=%d, %d trials, 1/e=%.3f, 1/n=%.3f]%n",
                n, trials, Strategy.SECRETARY_RATIO, 1.0 / n);
        for (final var strategy : List.of(Strategy.SAFE, Strategy.risk(0.5), Strategy.NEUTRAL, Strategy.risk(2), Strategy.RISKY)) {
            final var rng = new Random(42);                    // same sequences for every strategy — a fair race
            var wins = 0;
            for (var t = 0; t < trials; t++) {
                final var sequence = new ArrayList<Double>(n);
                for (var i = 0; i < n; i++) sequence.add(rng.nextDouble());
                final var choice = new OptimalStop<>(worth, strategy).apply(sequence);
                if (choice != null && choice.optimal()) wins++;
            }
            System.out.printf("  %-26s %.3f%n", strategy.name(), (double) wins / trials);
        }

        // 2. Compose with the current objects: court the contexts by thread-volume.
        if (args.length < 1) {
            System.out.println("\n(pass a data file to court a Plexus's contexts by thread-volume)");
            return;
        }
        final var data = Main.parse(Path.of(args[0]));
        final var side = data.isEmpty() ? 0 : (int) Math.round(Math.sqrt(data.get(0).grid().length));
        final List<Invariant> invariants = List.of(new Signature(new Mass(), new Connectivity(), side));
        final var plexus = Main.train(Dihedral.d4(), invariants, new ExactExceptions(), data);

        final var keys = new ArrayList<>(plexus.contexts().keySet());
        final Utility<Plexus.Key> volume = Utility.of("fiber-volume",
                k -> new ThreadVolume(k, side).apply(plexus).fiber());
        final var choice = new OptimalStop<>(volume).apply(keys);

        System.out.printf("%nCourting %d context(s) by thread-volume (|fiber|), 1/e rule:%n", keys.size());
        if (choice == null) { System.out.println("  (no contexts)"); return; }
        System.out.printf("  committed to:  %-22s |fiber|=%.0f  at position %d/%d  ->  %s%n",
                plexus.coordString(choice.chosen()), choice.value(), choice.index() + 1, choice.total(),
                choice.optimal() ? "the richest dowry (optimal)" : "not the richest (the 1/e gamble lost)");
    }
}
