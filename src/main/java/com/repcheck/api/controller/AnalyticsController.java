package com.repcheck.api.controller;

import com.repcheck.api.dto.AnalyticsDtos.BalanceResponse;
import com.repcheck.api.dto.AnalyticsDtos.FatigueResponse;
import com.repcheck.api.dto.AnalyticsDtos.HeatmapDay;
import com.repcheck.api.dto.AnalyticsDtos.HistoryPoint;
import com.repcheck.api.dto.AnalyticsDtos.LiftCard;
import com.repcheck.api.dto.AnalyticsDtos.PrRecord;
import com.repcheck.api.dto.AnalyticsDtos.WeeklyVolumeResponse;
import com.repcheck.api.security.AuthUser;
import com.repcheck.api.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** Latest state of every lift with its up / down / same arrow. */
    @GetMapping("/lifts")
    public List<LiftCard> lifts(@AuthenticationPrincipal AuthUser user) {
        return analyticsService.lifts(user.id());
    }

    @GetMapping("/exercises/{exerciseId}/history")
    public List<HistoryPoint> history(@AuthenticationPrincipal AuthUser user, @PathVariable UUID exerciseId) {
        return analyticsService.exerciseHistory(user.id(), exerciseId);
    }

    /** Sets per muscle for the week containing {@code date} (default: this week). */
    @GetMapping("/volume")
    public WeeklyVolumeResponse volume(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return analyticsService.weeklyVolume(user.id(), date);
    }

    @GetMapping("/fatigue")
    public FatigueResponse fatigue(@AuthenticationPrincipal AuthUser user,
                                   @RequestParam(defaultValue = "90") int days) {
        return analyticsService.fatigue(user.id(), days);
    }

    @GetMapping("/heatmap")
    public List<HeatmapDay> heatmap(@AuthenticationPrincipal AuthUser user,
                                    @RequestParam(defaultValue = "365") int days) {
        return analyticsService.heatmap(user.id(), days);
    }

    @GetMapping("/balance")
    public BalanceResponse balance(@AuthenticationPrincipal AuthUser user) {
        return analyticsService.balance(user.id());
    }

    @GetMapping("/prs")
    public List<PrRecord> personalRecords(@AuthenticationPrincipal AuthUser user) {
        return analyticsService.personalRecords(user.id());
    }
}
