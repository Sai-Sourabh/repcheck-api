package com.repcheck.api.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors,
                            Instant timestamp) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, Map.of(), Instant.now());
    }
}
