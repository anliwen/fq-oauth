package com.hczq.fq.common.api;

public record ResultResponse<T>(int code, String msg, T result) {
    public static <T> ResultResponse<T> success(T result) {
        return new ResultResponse<>(ResultCode.SUCCESS.code(), "success", result);
    }

    public static ResultResponse<Void> failure(ResultCode code, String msg) {
        return new ResultResponse<>(code.code(), msg, null);
    }
}
