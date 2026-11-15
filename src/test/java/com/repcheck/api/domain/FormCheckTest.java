package com.repcheck.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.repcheck.api.domain.FormCheck.Session;
import com.repcheck.api.domain.FormCheck.Warning;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormCheckTest {

    @Test
    void missingCleanCountMeansAllRepsWereClean() {
        assertThat(FormCheck.effectiveReps(8, null)).isEqualTo(8);
    }

    @Test
    void cleanRepsAreClampedToTheSet() {
        assertThat(FormCheck.effectiveReps(8, 6)).isEqualTo(6);
        assertThat(FormCheck.effectiveReps(8, 12)).isEqualTo(8);
        assertThat(FormCheck.effectiveReps(8, -1)).isZero();
    }

    @Test
    void brokenFormLowersTheEstimatedMax() {
        // 100 x 8 with form breaking after rep 6 is credited as 100 x 6.
        double credited = OneRMCalculator.estimate(100, FormCheck.effectiveReps(8, 6));
        assertThat(credited).isEqualTo(OneRMCalculator.estimate(100, 6));
        assertThat(credited).isLessThan(OneRMCalculator.estimate(100, 8));
    }

    @Test
    void formPercent() {
        assertThat(FormCheck.formPct(24, 18)).isEqualTo(75.0);
        assertThat(FormCheck.formPct(8, 5)).isEqualTo(62.5);
        assertThat(FormCheck.formPct(10, 10)).isEqualTo(100.0);
        assertThat(FormCheck.formPct(0, 0)).isNull();
    }

    @Test
    void twoSloppySessionsAfterAddingWeightSuggestTheLastCleanWeight() {
        Warning warning = FormCheck.egoLift(List.of(
                new Session(47.5, 18, 18), new Session(50, 18, 9), new Session(50, 18, 12)));
        assertThat(warning).isNotNull();
        assertThat(warning.suggestedWeightKg()).isEqualTo(47.5);
        assertThat(warning.message()).contains("47.5 kg");
    }

    @Test
    void wholeNumberWeightsPrintWithoutDecimals() {
        Warning warning = FormCheck.egoLift(List.of(
                new Session(40, 8, 8), new Session(45, 8, 5), new Session(45, 8, 6)));
        assertThat(warning.message()).contains("Drop back to 40 kg");
    }

    @Test
    void oneSloppySessionIsNotAWarning() {
        assertThat(FormCheck.egoLift(List.of(new Session(40, 8, 8), new Session(45, 8, 5)))).isNull();
        assertThat(FormCheck.egoLift(List.of(new Session(45, 8, 5)))).isNull();
        assertThat(FormCheck.egoLift(List.of())).isNull();
    }

    @Test
    void recoveringFormClearsTheWarning() {
        assertThat(FormCheck.egoLift(List.of(
                new Session(45, 8, 5), new Session(45, 8, 5), new Session(45, 8, 8)))).isNull();
    }

    @Test
    void sloppyAtTheSameWeightGivesGenericAdvice() {
        Warning warning = FormCheck.egoLift(List.of(
                new Session(50, 8, 8), new Session(50, 8, 5), new Session(50, 8, 5)));
        assertThat(warning).isNotNull();
        assertThat(warning.suggestedWeightKg()).isNull();
    }

    @Test
    void noCleanBaselineGivesGenericAdvice() {
        Warning warning = FormCheck.egoLift(List.of(new Session(50, 8, 5), new Session(50, 8, 4)));
        assertThat(warning).isNotNull();
        assertThat(warning.suggestedWeightKg()).isNull();
    }

    @Test
    void exactlyAtTheThresholdCountsAsClean() {
        assertThat(FormCheck.egoLift(List.of(new Session(50, 10, 8), new Session(50, 10, 8)))).isNull();
    }
}
