package com.repcheck.api.controller;

import com.repcheck.api.dto.TemplateDtos.TemplateRequest;
import com.repcheck.api.dto.TemplateDtos.TemplateResponse;
import com.repcheck.api.security.AuthUser;
import com.repcheck.api.service.TemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
@Tag(name = "Templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<TemplateResponse> list(@AuthenticationPrincipal AuthUser user) {
        return templateService.list(user.id());
    }

    @GetMapping("/{id}")
    public TemplateResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return templateService.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(@AuthenticationPrincipal AuthUser user,
                                   @Valid @RequestBody TemplateRequest request) {
        return templateService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public TemplateResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                   @Valid @RequestBody TemplateRequest request) {
        return templateService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        templateService.delete(user.id(), id);
    }
}
