package com.hczq.fq.web;

import com.hczq.fq.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

/**
 * Error envelope: {code, message, data: null, details}. Validation failures never echo the submitted values
 * (they may contain passwords).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(String code, String message, Object data, Object details) {
    }

    private static ResponseEntity<ErrorBody> build(int status, String code, String message, Object details) {
        return ResponseEntity.status(status).body(new ErrorBody(code, message, null, details));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorBody> business(BusinessException e) {
        log.warn("business error: {}", e.getMessage());
        return build(e.getStatus(), e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException e) {
        List<Map<String, String>> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("loc", f.getField(), "msg", String.valueOf(f.getDefaultMessage())))
                .toList();
        return build(422, "VALIDATION_ERROR", "请求参数校验失败", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorBody> unreadable(Exception e) {
        return build(422, "VALIDATION_ERROR", "请求参数格式错误", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorBody> noResource(NoResourceFoundException e) {
        return build(404, "HTTP_ERROR", "资源不存在", null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorBody> status(ResponseStatusException e) {
        HttpStatus s = HttpStatus.resolve(e.getStatusCode().value());
        return build(e.getStatusCode().value(), "HTTP_ERROR", e.getReason() != null ? e.getReason()
                : s != null ? s.getReasonPhrase() : "请求失败", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> unexpected(Exception e) {
        log.error("unexpected error", e);
        return build(500, "INTERNAL_ERROR", "服务器内部错误", null);
    }
}
