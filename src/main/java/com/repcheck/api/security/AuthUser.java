package com.repcheck.api.security;

import java.util.UUID;

/** The authenticated caller, taken from the JWT. */
public record AuthUser(UUID id, String email) {
}
