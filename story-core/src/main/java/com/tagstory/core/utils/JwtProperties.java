package com.tagstory.core.utils;

public final class JwtProperties {
    private JwtProperties() {}

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String TOKEN_TYPE_ACCESS = "AccessToken";
    public static final String TOKEN_TYPE_REFRESH = "RefreshToken";
    public static final String TOKEN_TYPE_PENDING = "Pending";
}
