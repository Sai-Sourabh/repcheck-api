package com.repcheck.api.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Weeks run Monday to Sunday. */
public final class Weeks {

    private Weeks() {
    }

    public static LocalDate startOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate endOf(LocalDate weekStart) {
        return weekStart.plusDays(6);
    }
}
