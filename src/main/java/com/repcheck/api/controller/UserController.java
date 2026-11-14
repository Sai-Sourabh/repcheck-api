package com.repcheck.api.controller;

import com.repcheck.api.dto.AuthDtos.UpdateProfileRequest;
import com.repcheck.api.dto.AuthDtos.UserResponse;
import com.repcheck.api.security.AuthUser;
import com.repcheck.api.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "User")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/health")
    @SecurityRequirements
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/users/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return userService.me(user.id());
    }

    @PatchMapping("/users/me")
    public UserResponse update(@AuthenticationPrincipal AuthUser user,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return userService.update(user.id(), request);
    }
}
