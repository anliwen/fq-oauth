package com.hczq.fq.common.api;

/** Stable numeric result codes for public OAuth endpoints. */
public enum ResultCode {
    SUCCESS(0),
    CLIENT_SECRET_INVALID(30001),
    LOGIN_FAILED(30002),
    PLATFORM_INVALID(30003),
    PLATFORM_ROLE_MISSING(30004),
    CLIENT_TYPE_INVALID(30005),
    REQUEST_INVALID(30006),
    TOKEN_INVALID(30007),
    ACCESS_DENIED(30008),
    INTERNAL_ERROR(30099);

    private final int code;

    ResultCode(int code) { this.code = code; }

    public int code() { return code; }
}
