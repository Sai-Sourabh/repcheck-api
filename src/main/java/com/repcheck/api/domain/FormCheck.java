package com.repcheck.api.domain;

import java.util.List;

/**
 * Form tracking. A set records how many of its reps were clean; only clean
 * reps count toward estimated 1RM, arrows and PRs. All reps still count
 * toward volume and fatigue, because sloppy reps tire you just the same.
 */
public final class FormCheck {

    /** A session below this share of clean reps counts as sloppy. */
    public static final double CLEAN_THRESHOLD_PCT = 80.0;

    /** One session of one exercise. */
    public record Session(double topWeightKg, int totalReps, int cleanReps) {
    }

    public record Warning(String message, Double suggestedWeightKg) {
    }

    private FormCheck() {
    }

    /** Reps that count as progress. A missing clean count means every rep was clean. */
    public static int effectiveReps(int reps, Integer cleanReps) {
        if (cleanReps == null) {
            return reps;
        }
        return Math.min(Math.max(cleanReps, 0), reps);
    }

    /** Share of reps done clean, 0 to 100 with one decimal. Null when there are no reps. */
    public static Double formPct(int totalReps, int cleanReps) {
        if (totalReps <= 0) {
            return null;
        }
        return Math.round(cleanReps * 1000.0 / totalReps) / 10.0;
    }

    /**
     * Ego-lift check: form under the threshold for the last two sessions in a row.
     * If an earlier clean session used a lighter weight, that weight is suggested.
     *
     * @param sessions oldest first
     * @return a warning, or null when form is fine
     */
    public static Warning egoLift(List<Session> sessions) {
        int n = sessions.size();
        if (n < 2 || !sloppy(sessions.get(n - 1)) || !sloppy(sessions.get(n - 2))) {
            return null;
        }
        Session latest = sessions.get(n - 1);
        for (int i = n - 3; i >= 0; i--) {
            Session earlier = sessions.get(i);
            if (sloppy(earlier)) {
                continue;
            }
            if (earlier.topWeightKg() < latest.topWeightKg()) {
                return new Warning("Form has broken down for 2 sessions since the weight went up. Drop back to "
                        + weight(earlier.topWeightKg()) + " kg and own every rep.", earlier.topWeightKg());
            }
            break;
        }
        return new Warning("Form has broken down for 2 sessions in a row. "
                + "Lower the weight until every rep is clean.", null);
    }

    private static boolean sloppy(Session session) {
        Double pct = formPct(session.totalReps(), session.cleanReps());
        return pct != null && pct < CLEAN_THRESHOLD_PCT;
    }

    private static String weight(double kg) {
        return kg == Math.rint(kg) ? String.valueOf((long) kg) : String.valueOf(kg);
    }
}
