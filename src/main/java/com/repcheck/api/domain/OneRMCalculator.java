package com.repcheck.api.domain;

/**
 * Estimated one-rep max (Epley) and the session-to-session delta that drives
 * the green / red / yellow arrows.
 */
public final class OneRMCalculator {

    /** Changes smaller than this (in percent) count as "same". */
    public static final double SAME_THRESHOLD_PCT = 0.5;

    public enum Direction { UP, DOWN, SAME, NEW }

    public record Delta(Double pct, Direction direction) {
    }

    private OneRMCalculator() {
    }

    /** Epley: weight x (1 + reps / 30). A single rep is its own max. */
    public static double estimate(double weightKg, int reps) {
        if (weightKg <= 0 || reps <= 0) {
            return 0;
        }
        if (reps == 1) {
            return round2(weightKg);
        }
        return round2(weightKg * (1 + reps / 30.0));
    }

    /**
     * @param previous metric from the previous session, or null if there is none
     * @param current  metric from this session
     */
    public static Delta delta(Double previous, double current) {
        if (previous == null) {
            return new Delta(null, Direction.NEW);
        }
        if (previous == 0) {
            return current > 0 ? new Delta(100.0, Direction.UP) : new Delta(0.0, Direction.SAME);
        }
        double pct = round2((current - previous) / previous * 100.0);
        if (Math.abs(pct) < SAME_THRESHOLD_PCT) {
            return new Delta(pct, Direction.SAME);
        }
        return new Delta(pct, pct > 0 ? Direction.UP : Direction.DOWN);
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
