package com.repcheck.api.domain;

import com.repcheck.api.domain.VolumeLandmarks.Landmarks;

/**
 * Decides whether next week's volume for a muscle should go up, hold or drop.
 * Rules are checked top to bottom; the first match wins.
 */
public final class ProgressionRuleEngine {

    /** Average lift change (percent) below this counts as a real drop. */
    public static final double DROP_THRESHOLD_PCT = -1.0;
    public static final double DELOAD_FACTOR = 0.6;
    public static final double INCREASE_FACTOR = 1.05;

    public enum Action { INCREASE, MAINTAIN, DELOAD }

    public record Recommendation(Action action, String reason, int suggestedSets) {
    }

    private ProgressionRuleEngine() {
    }

    /**
     * @param landmarks   MEV / MAV / MRV for the muscle
     * @param weeklySets  effective sets done this week
     * @param avgDeltaPct average 1RM change of this muscle's lifts this week, null if nothing to compare
     * @param tsbPct      TSB as percent of CTL, null while the baseline is still building
     */
    public static Recommendation recommend(Landmarks landmarks, double weeklySets,
                                           Double avgDeltaPct, Double tsbPct) {
        int done = (int) Math.round(weeklySets);

        if (weeklySets <= 0) {
            return new Recommendation(Action.INCREASE,
                    "No sets this week. Aim for at least " + landmarks.mev() + ".", landmarks.mev());
        }
        if (tsbPct != null && tsbPct < FatigueCalculator.OVERREACHING_PCT) {
            return new Recommendation(Action.DELOAD,
                    "Fatigue is well above your baseline. Cut volume for a week.", deload(weeklySets));
        }
        if (weeklySets > landmarks.mrv()) {
            return new Recommendation(Action.DELOAD,
                    "Above the most you can recover from (" + landmarks.mrv() + " sets).", landmarks.mav());
        }
        if (weeklySets < landmarks.mev()) {
            return new Recommendation(Action.INCREASE,
                    "Below the minimum that drives growth (" + landmarks.mev() + " sets).",
                    Math.min(landmarks.mev(), done + 2));
        }
        if (avgDeltaPct == null) {
            return new Recommendation(Action.MAINTAIN,
                    "Not enough history to compare yet. Repeat this week.", done);
        }
        if (avgDeltaPct < DROP_THRESHOLD_PCT) {
            if (weeklySets >= landmarks.mav()) {
                return new Recommendation(Action.DELOAD,
                        "Lifts dropped at high volume. Back off and recover.", deload(weeklySets));
            }
            return new Recommendation(Action.MAINTAIN,
                    "Lifts dipped. Hold volume and let strength come back.", done);
        }
        if (weeklySets < landmarks.mav()) {
            int more = (int) Math.max(Math.ceil(weeklySets * INCREASE_FACTOR), Math.floor(weeklySets) + 1);
            return new Recommendation(Action.INCREASE,
                    "Lifts are holding or rising and you have room to recover.",
                    Math.min(landmarks.mav(), more));
        }
        return new Recommendation(Action.MAINTAIN,
                "Productive volume, close to your ceiling. Push weight, not sets.", done);
    }

    private static int deload(double weeklySets) {
        return Math.max(1, (int) Math.round(weeklySets * DELOAD_FACTOR));
    }
}
