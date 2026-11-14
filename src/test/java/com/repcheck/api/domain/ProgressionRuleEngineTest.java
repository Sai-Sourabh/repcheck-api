package com.repcheck.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.repcheck.api.domain.ProgressionRuleEngine.Action;
import com.repcheck.api.domain.ProgressionRuleEngine.Recommendation;
import com.repcheck.api.domain.VolumeLandmarks.Landmarks;
import org.junit.jupiter.api.Test;

class ProgressionRuleEngineTest {

    /** MEV 8, MAV 16, MRV 22. */
    private static final Landmarks CHEST = VolumeLandmarks.forMuscle("CHEST");

    @Test
    void noSetsMeansStartAtMinimum() {
        Recommendation rec = ProgressionRuleEngine.recommend(CHEST, 0, null, null);
        assertThat(rec.action()).isEqualTo(Action.INCREASE);
        assertThat(rec.suggestedSets()).isEqualTo(8);
    }

    @Test
    void belowMinimumIncreasesTowardsIt() {
        Recommendation rec = ProgressionRuleEngine.recommend(CHEST, 4, 2.0, 0.0);
        assertThat(rec.action()).isEqualTo(Action.INCREASE);
        assertThat(rec.suggestedSets()).isEqualTo(6);
    }

    @Test
    void liftsRisingWithRoomToRecoverIncreases() {
        Recommendation rec = ProgressionRuleEngine.recommend(CHEST, 12, 2.5, -5.0);
        assertThat(rec.action()).isEqualTo(Action.INCREASE);
        assertThat(rec.suggestedSets()).isEqualTo(13);
    }

    @Test
    void increaseNeverGoesPastTopOfProductiveRange() {
        Recommendation rec = ProgressionRuleEngine.recommend(CHEST, 15.5, 1.0, 0.0);
        assertThat(rec.action()).isEqualTo(Action.INCREASE);
        assertThat(rec.suggestedSets()).isEqualTo(16);
    }

    @Test
    void steadyLiftsStillCountAsProgressable() {
        assertThat(ProgressionRuleEngine.recommend(CHEST, 10, 0.0, 0.0).action()).isEqualTo(Action.INCREASE);
    }

    @Test
    void highVolumeWithRisingLiftsHolds() {
        Recommendation rec = ProgressionRuleEngine.recommend(CHEST, 18, 3.0, -5.0);
        assertThat(rec.action()).isEqualTo(Action.MAINTAIN);
        assertThat(rec.suggestedSets()).isEqualTo(18);
    }

    @Test
    void droppingLiftsAtModerateVolumeHolds() {
        assertThat(ProgressionRuleEngine.recommend(CHEST, 12, -3.0, -5.0).action()).isEqualTo(Action.MAINTAIN);
    }

    @Test
    void droppingLiftsAtHighVolumeDeloads() {
        Recommendation rec = ProgressionRuleEngine.recommend(CHEST, 20, -3.0, -5.0);
        assertThat(rec.action()).isEqualTo(Action.DELOAD);
        assertThat(rec.suggestedSets()).isEqualTo(12);
    }

    @Test
    void aboveMaximumRecoverableDeloads() {
        Recommendation rec = ProgressionRuleEngine.recommend(CHEST, 26, 4.0, 0.0);
        assertThat(rec.action()).isEqualTo(Action.DELOAD);
        assertThat(rec.suggestedSets()).isEqualTo(16);
    }

    @Test
    void heavyFatigueDeloadsEvenWhenLiftsRise() {
        Recommendation rec = ProgressionRuleEngine.recommend(CHEST, 14, 5.0, -45.0);
        assertThat(rec.action()).isEqualTo(Action.DELOAD);
        assertThat(rec.suggestedSets()).isEqualTo(8);
    }

    @Test
    void fatigueIsIgnoredWhileBaselineIsBuilding() {
        assertThat(ProgressionRuleEngine.recommend(CHEST, 12, 2.0, null).action()).isEqualTo(Action.INCREASE);
    }

    @Test
    void nothingToCompareHolds() {
        assertThat(ProgressionRuleEngine.recommend(CHEST, 12, null, null).action()).isEqualTo(Action.MAINTAIN);
    }

    @Test
    void smallDipIsNotADrop() {
        assertThat(ProgressionRuleEngine.recommend(CHEST, 12, -0.8, 0.0).action()).isEqualTo(Action.INCREASE);
    }

    @Test
    void everyKnownMuscleHasOrderedLandmarks() {
        for (String muscle : VolumeLandmarks.muscleGroups()) {
            Landmarks landmarks = VolumeLandmarks.forMuscle(muscle);
            assertThat(landmarks.mev()).isLessThan(landmarks.mav());
            assertThat(landmarks.mav()).isLessThan(landmarks.mrv());
        }
        assertThat(VolumeLandmarks.muscleGroups()).hasSize(10);
    }
}
