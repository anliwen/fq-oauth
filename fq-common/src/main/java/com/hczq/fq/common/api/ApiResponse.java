package com.hczq.fq.common.api;

/** Unified response envelope; {@code code} is "OK" on success. */
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>("OK", message, null);
    }
}
