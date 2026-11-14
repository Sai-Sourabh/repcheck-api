package com.repcheck.api.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Fitness / fatigue model on daily training volume.
 *
 * <ul>
 *   <li>ATL (acute load): 7 day exponentially weighted average. How tired you are.</li>
 *   <li>CTL (chronic load): 28 day exponentially weighted average. Your fitness base.</li>
 *   <li>TSB = CTL - ATL. Positive is fresh, negative is fatigued.</li>
 * </ul>
 *
 * Volume differs hugely between lifters, so decisions use TSB as a percentage
 * of CTL rather than the raw number.
 */
public final class FatigueCalculator {

    public static final int ATL_DAYS = 7;
    public static final int CTL_DAYS = 28;
    /** Days used to seed the averages so a new user does not start "overreaching". */
    public static final int SEED_DAYS = 14;

    public static final double OVERREACHING_PCT = -30.0;
    public static final double FATIGUED_PCT = -10.0;
    public static final double FRESH_PCT = 10.0;

    public enum Status { BUILDING_BASELINE, FRESH, BALANCED, FATIGUED, OVERREACHING }

    public record Point(double atl, double ctl, double tsb) {
    }

    private FatigueCalculator() {
    }

    static double alpha(int days) {
        return 1 - Math.exp(-1.0 / days);
    }

    public static Point next(double previousAtl, double previousCtl, double load) {
        double atl = previousAtl + (load - previousAtl) * alpha(ATL_DAYS);
        double ctl = previousCtl + (load - previousCtl) * alpha(CTL_DAYS);
        return new Point(atl, ctl, ctl - atl);
    }

    /** Runs the model over consecutive daily loads, starting from the given averages. */
    public static List<Point> series(double startAtl, double startCtl, List<Double> dailyLoads) {
        List<Point> points = new ArrayList<>(dailyLoads.size());
        double atl = startAtl;
        double ctl = startCtl;
        for (double load : dailyLoads) {
            Point point = next(atl, ctl, load);
            points.add(point);
            atl = point.atl();
            ctl = point.ctl();
        }
        return points;
    }

    /** Mean of the first {@link #SEED_DAYS} days, used as the starting ATL and CTL. */
    public static double seed(List<Double> dailyLoads) {
        int n = Math.min(SEED_DAYS, dailyLoads.size());
        if (n == 0) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += dailyLoads.get(i);
        }
        return sum / n;
    }

    /** TSB as a percentage of CTL, or null when there is no baseline yet. */
    public static Double tsbPercent(double atl, double ctl) {
        if (ctl < 1e-6) {
            return null;
        }
        return (ctl - atl) / ctl * 100.0;
    }

    public static Status status(double atl, double ctl, long daysOfData) {
        Double pct = tsbPercent(atl, ctl);
        if (pct == null || daysOfData < SEED_DAYS) {
            return Status.BUILDING_BASELINE;
        }
        if (pct < OVERREACHING_PCT) {
            return Status.OVERREACHING;
        }
        if (pct < FATIGUED_PCT) {
            return Status.FATIGUED;
        }
        if (pct > FRESH_PCT) {
            return Status.FRESH;
        }
        return Status.BALANCED;
    }
}
