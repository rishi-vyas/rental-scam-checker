package com.rishivyas.rentalscam.engine;

import com.rishivyas.rentalscam.domain.Signal;

import java.util.Collection;

/**
 * Combines signal points as independent pieces of evidence:
 * {@code score = 100 * (1 - Π(1 - points_i / 100))}.
 *
 * <p>Unlike a plain sum this never exceeds 100, a single critical signal dominates, and many
 * weak signals add up without one trivial flag pushing a listing into the red.
 */
public final class RiskScorer {

    private RiskScorer() {
    }

    public static int score(Collection<Signal> signals) {
        double clean = 1.0;
        for (Signal s : signals) {
            clean *= 1.0 - (s.points() / 100.0);
        }
        return (int) Math.round(100.0 * (1.0 - clean));
    }
}
