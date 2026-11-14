package com.repcheck.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.repcheck.api.entity.Streak;
import org.junit.jupiter.api.Test;

class StreakScoringTest {

    private static Streak streak(int current, int tokens) {
        Streak streak = new Streak();
        streak.setCurrentStreak(current);
        streak.setLongestStreak(current);
        streak.setFreezeTokens(tokens);
        return streak;
    }

    @Test
    void hittingTheGoalExtendsTheStreak() {
        Streak streak = streak(2, 0);
        StreakService.score(streak, true);
        assertThat(streak.getCurrentStreak()).isEqualTo(3);
        assertThat(streak.getLongestStreak()).isEqualTo(3);
    }

    @Test
    void missedWeekBurnsAFreezeTokenAndKeepsTheStreak() {
        Streak streak = streak(5, 1);
        StreakService.score(streak, false);
        assertThat(streak.getCurrentStreak()).isEqualTo(5);
        assertThat(streak.getFreezeTokens()).isZero();
    }

    @Test
    void missedWeekWithoutTokensResets() {
        Streak streak = streak(5, 0);
        StreakService.score(streak, false);
        assertThat(streak.getCurrentStreak()).isZero();
        assertThat(streak.getLongestStreak()).isEqualTo(5);
    }

    @Test
    void tokensAreNotWastedWhenThereIsNoStreak() {
        Streak streak = streak(0, 1);
        StreakService.score(streak, false);
        assertThat(streak.getFreezeTokens()).isEqualTo(1);
    }

    @Test
    void everyFourthWeekEarnsATokenUpToTheCap() {
        Streak streak = streak(3, 0);
        StreakService.score(streak, true);
        assertThat(streak.getFreezeTokens()).isEqualTo(1);

        streak = streak(7, StreakService.MAX_FREEZE_TOKENS);
        StreakService.score(streak, true);
        assertThat(streak.getFreezeTokens()).isEqualTo(StreakService.MAX_FREEZE_TOKENS);
    }
}
