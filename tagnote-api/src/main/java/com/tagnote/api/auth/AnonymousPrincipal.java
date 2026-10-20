package com.tagnote.api.auth;

/** Carries a null user ID for controller argument resolution without granting a user role. */
public final class AnonymousPrincipal {
    public Long getUserId() {
        return null;
    }
}
