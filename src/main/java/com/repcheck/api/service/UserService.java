package com.repcheck.api.service;

import com.repcheck.api.dto.AuthDtos.AuthResponse;
import com.repcheck.api.dto.AuthDtos.LoginRequest;
import com.repcheck.api.dto.AuthDtos.RegisterRequest;
import com.repcheck.api.dto.AuthDtos.UpdateProfileRequest;
import com.repcheck.api.dto.AuthDtos.UserResponse;
import com.repcheck.api.entity.User;
import com.repcheck.api.exception.ApiException;
import com.repcheck.api.repository.UserRepository;
import com.repcheck.api.security.JwtService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with that email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        users.save(user);
        return new AuthResponse(jwtService.generate(user.getId(), user.getEmail()), toResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Wrong email or password"));
        return new AuthResponse(jwtService.generate(user.getId(), user.getEmail()), toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return toResponse(require(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateProfileRequest request) {
        User user = require(userId);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.weeklyStepsGoal() != null) {
            user.setWeeklyStepsGoal(request.weeklyStepsGoal());
        }
        if (request.weeklyWorkoutGoal() != null) {
            user.setWeeklyWorkoutGoal(request.weeklyWorkoutGoal());
        }
        user.setUpdatedAt(Instant.now());
        return toResponse(user);
    }

    public User require(UUID userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getBodyWeightKg(),
                user.getWeeklyStepsGoal(), user.getWeeklyWorkoutGoal());
    }
}
