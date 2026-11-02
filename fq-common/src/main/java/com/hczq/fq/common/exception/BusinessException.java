package com.hczq.fq.common.exception;

/** Business error carrying the HTTP status and a machine-readable code. */
public class BusinessException extends RuntimeException {

    private final String code;
    private final int status;

    public BusinessException(String message, String code, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(message, "BAD_REQUEST", 400);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, "UNAUTHORIZED", 401);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, "FORBIDDEN", 403);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(message, "NOT_FOUND", 404);
    }

    public static BusinessException invalid(String message) {
        return new BusinessException(message, "VALIDATION_ERROR", 422);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(message, "CONFLICT", 409);
    }
}
