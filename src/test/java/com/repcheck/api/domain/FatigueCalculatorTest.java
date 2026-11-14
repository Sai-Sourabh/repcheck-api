package com.repcheck.api.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.repcheck.api.domain.FatigueCalculator.Point;
import com.repcheck.api.domain.FatigueCalculator.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FatigueCalculatorTest {

    private static List<Double> constant(double load, int days) {
        return new ArrayList<>(Collections.nCopies(days, load));
    }

    private static Point last(List<Point> points) {
        return points.get(points.size() - 1);
    }

    @Test
    void steadyLoadConvergesAndBalances() {
        Point point = last(FatigueCalculator.series(0, 0, constant(5000, 200)));
        assertThat(point.atl()).isCloseTo(5000, within(1.0));
        assertThat(point.ctl()).isCloseTo(5000, within(10.0));
        assertThat(point.tsb()).isCloseTo(0, within(10.0));
    }

    @Test
    void acuteLoadReactsFasterThanChronic() {
        Point point = last(FatigueCalculator.series(0, 0, constant(5000, 7)));
        assertThat(point.atl()).isGreaterThan(point.ctl());
        assertThat(point.tsb()).isNegative();
    }

    @Test
    void tsbIsChronicMinusAcute() {
        Point point = FatigueCalculator.next(3000, 4000, 6000);
        assertThat(point.tsb()).isCloseTo(point.ctl() - point.atl(), within(1e-9));
    }

    @Test
    void restMakesYouFresh() {
        List<Double> loads = constant(5000, 60);
        loads.addAll(constant(0, 7));
        Point point = last(FatigueCalculator.series(5000, 5000, loads));
        assertThat(point.tsb()).isPositive();
        assertThat(FatigueCalculator.status(point.atl(), point.ctl(), 67)).isEqualTo(Status.FRESH);
    }

    @Test
    void doublingVolumeOverreaches() {
        List<Double> loads = constant(4000, 60);
        loads.addAll(constant(9000, 7));
        Point point = last(FatigueCalculator.series(4000, 4000, loads));
        assertThat(FatigueCalculator.tsbPercent(point.atl(), point.ctl()))
                .isLessThan(FatigueCalculator.OVERREACHING_PCT);
        assertThat(FatigueCalculator.status(point.atl(), point.ctl(), 67)).isEqualTo(Status.OVERREACHING);
    }

    @Test
    void seedingAvoidsFalseOverreachingForNewUsers() {
        // Three sessions a week from day one. Unseeded, week three reads as overreaching.
        List<Double> loads = new ArrayList<>();
        for (int day = 0; day < 21; day++) {
            loads.add(day % 7 == 0 || day % 7 == 2 || day % 7 == 4 ? 6000.0 : 0.0);
        }
        double seed = FatigueCalculator.seed(loads);
        Point seeded = last(FatigueCalculator.series(seed, seed, loads));
        Point unseeded = last(FatigueCalculator.series(0, 0, loads));

        assertThat(FatigueCalculator.tsbPercent(unseeded.atl(), unseeded.ctl()))
                .isLessThan(FatigueCalculator.OVERREACHING_PCT);
        assertThat(FatigueCalculator.status(seeded.atl(), seeded.ctl(), 21))
                .isNotEqualTo(Status.OVERREACHING);
    }

    @Test
    void seedIsMeanOfFirstTwoWeeks() {
        List<Double> loads = constant(1000, 14);
        loads.addAll(constant(9000, 14));
        assertThat(FatigueCalculator.seed(loads)).isEqualTo(1000.0);
        assertThat(FatigueCalculator.seed(List.of(300.0, 0.0, 600.0))).isEqualTo(300.0);
        assertThat(FatigueCalculator.seed(List.of())).isZero();
    }

    @Test
    void noBaselineMeansNoPercentAndBuildingStatus() {
        assertThat(FatigueCalculator.tsbPercent(0, 0)).isNull();
        assertThat(FatigueCalculator.status(0, 0, 30)).isEqualTo(Status.BUILDING_BASELINE);
        assertThat(FatigueCalculator.status(5000, 5000, 5)).isEqualTo(Status.BUILDING_BASELINE);
    }

    @Test
    void statusBands() {
        assertThat(FatigueCalculator.status(100, 100, 30)).isEqualTo(Status.BALANCED);
        assertThat(FatigueCalculator.status(120, 100, 30)).isEqualTo(Status.FATIGUED);
        assertThat(FatigueCalculator.status(140, 100, 30)).isEqualTo(Status.OVERREACHING);
        assertThat(FatigueCalculator.status(80, 100, 30)).isEqualTo(Status.FRESH);
    }
}
