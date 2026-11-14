package com.repcheck.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 100) String displayName) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UserResponse(UUID id, String email, String displayName, Double bodyWeightKg,
                               int weeklyStepsGoal, int weeklyWorkoutGoal) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record UpdateProfileRequest(
            @Size(min = 1, max = 100) String displayName,
            @Min(1000) @Max(500000) Integer weeklyStepsGoal,
            @Min(1) @Max(14) Integer weeklyWorkoutGoal) {
    }
}
