package com.repcheck.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.repcheck.api.domain.OneRMCalculator.Delta;
import com.repcheck.api.domain.OneRMCalculator.Direction;
import org.junit.jupiter.api.Test;

class OneRMCalculatorTest {

    @Test
    void epleyFormula() {
        assertThat(OneRMCalculator.estimate(100, 5)).isEqualTo(116.67);
        assertThat(OneRMCalculator.estimate(60, 10)).isEqualTo(80.0);
        assertThat(OneRMCalculator.estimate(82.5, 8)).isEqualTo(104.5);
    }

    @Test
    void singleRepIsItsOwnMax() {
        assertThat(OneRMCalculator.estimate(140, 1)).isEqualTo(140.0);
    }

    @Test
    void zeroOrNegativeInputGivesZero() {
        assertThat(OneRMCalculator.estimate(0, 12)).isZero();
        assertThat(OneRMCalculator.estimate(-5, 5)).isZero();
        assertThat(OneRMCalculator.estimate(50, 0)).isZero();
    }

    @Test
    void sameWeightMoreRepsBeatsFewerReps() {
        assertThat(OneRMCalculator.estimate(80, 8)).isGreaterThan(OneRMCalculator.estimate(80, 6));
    }

    @Test
    void differentRepRangesAreComparable() {
        // 5x100 one week vs 8x90 the next: raw weight went down, strength went up.
        double heavy = OneRMCalculator.estimate(100, 5);
        double lighter = OneRMCalculator.estimate(90, 8);
        assertThat(OneRMCalculator.delta(heavy, lighter).direction()).isEqualTo(Direction.DOWN);
        assertThat(OneRMCalculator.delta(heavy, OneRMCalculator.estimate(95, 8)).direction())
                .isEqualTo(Direction.UP);
    }

    @Test
    void firstSessionIsNew() {
        Delta delta = OneRMCalculator.delta(null, 100);
        assertThat(delta.direction()).isEqualTo(Direction.NEW);
        assertThat(delta.pct()).isNull();
    }

    @Test
    void upAndDown() {
        assertThat(OneRMCalculator.delta(100.0, 105)).isEqualTo(new Delta(5.0, Direction.UP));
        assertThat(OneRMCalculator.delta(100.0, 92.5)).isEqualTo(new Delta(-7.5, Direction.DOWN));
    }

    @Test
    void tinyChangesCountAsSame() {
        assertThat(OneRMCalculator.delta(100.0, 100).direction()).isEqualTo(Direction.SAME);
        assertThat(OneRMCalculator.delta(100.0, 100.4).direction()).isEqualTo(Direction.SAME);
        assertThat(OneRMCalculator.delta(100.0, 99.6).direction()).isEqualTo(Direction.SAME);
        assertThat(OneRMCalculator.delta(100.0, 100.5).direction()).isEqualTo(Direction.UP);
    }

    @Test
    void previousZeroDoesNotDivideByZero() {
        assertThat(OneRMCalculator.delta(0.0, 50).direction()).isEqualTo(Direction.UP);
        assertThat(OneRMCalculator.delta(0.0, 0).direction()).isEqualTo(Direction.SAME);
    }
}
