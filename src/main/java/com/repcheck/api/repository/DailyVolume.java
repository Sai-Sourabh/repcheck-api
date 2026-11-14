package com.repcheck.api.repository;

import java.time.LocalDate;

public record DailyVolume(LocalDate date, Double volumeKg, Long sessions) {
}
