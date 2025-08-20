package com.doclab.doclab.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    // ---- Public handlers ----

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> handleRse(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return withTraceHeader(status, err(ex.getReason(), status, req));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .findFirst().orElse("Invalid request");
        return withTraceHeader(HttpStatus.BAD_REQUEST, err(msg, HttpStatus.BAD_REQUEST, req));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTooBig(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return withTraceHeader(HttpStatus.PAYLOAD_TOO_LARGE,
                err("Upload too large", HttpStatus.PAYLOAD_TOO_LARGE, req));
    }

    // NEW: common app bad-request path
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return withTraceHeader(HttpStatus.BAD_REQUEST, err(ex.getMessage(), HttpStatus.BAD_REQUEST, req));
    }

    // NEW: NLP upstream network issues
    @ExceptionHandler(SocketTimeoutException.class)
    public ResponseEntity<Map<String,Object>> handleTimeout(SocketTimeoutException ex, HttpServletRequest req) {
        return withTraceHeader(HttpStatus.GATEWAY_TIMEOUT,
                err("NLP service timeout", HttpStatus.GATEWAY_TIMEOUT, req));
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<Map<String,Object>> handleConnect(ConnectException ex, HttpServletRequest req) {
        return withTraceHeader(HttpStatus.BAD_GATEWAY,
                err("NLP service unavailable", HttpStatus.BAD_GATEWAY, req));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex, HttpServletRequest req) {
        return withTraceHeader(HttpStatus.INTERNAL_SERVER_ERROR,
                err(ex.getMessage() != null ? ex.getMessage() : "Unexpected error",
                        HttpStatus.INTERNAL_SERVER_ERROR, req));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(
            org.springframework.web.HttpMediaTypeNotSupportedException ex,
            HttpServletRequest req) {

        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE; // 415
        String msg = "Unsupported Content-Type: " + ex.getContentType();
        return ResponseEntity.status(status).body(err(msg, status, req));
    }

    // 400 for @ModelAttribute binding errors (e.g., @NotNull on file)
    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<Map<String, Object>> handleBind(org.springframework.validation.BindException ex,
                                                          HttpServletRequest req) {
        String msg = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .findFirst().orElse("Invalid request");
        return withTraceHeader(HttpStatus.BAD_REQUEST, err(msg, HttpStatus.BAD_REQUEST, req));
    }

    // 400 when required multipart part is missing (e.g., no "file" form field)
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingPart(
            org.springframework.web.multipart.support.MissingServletRequestPartException ex,
            HttpServletRequest req) {
        String part = ex.getRequestPartName();
        String msg = (part != null) ? (part + " is required") : "Missing multipart part";
        return withTraceHeader(HttpStatus.BAD_REQUEST, err(msg, HttpStatus.BAD_REQUEST, req));
    }

    // ---- Helpers ----

    private Map<String, Object> err(String message, HttpStatus status, HttpServletRequest req) {
        String trace = MDC.get("traceId");
        if (trace == null) trace = "n/a";
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "traceId", trace,
                "path", req.getRequestURI()
        );
    }

    private ResponseEntity<Map<String,Object>> withTraceHeader(HttpStatus status, Map<String,Object> body) {
        String trace = String.valueOf(body.get("traceId"));
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Trace-Id", trace);
        return new ResponseEntity<>(body, headers, status);
    }
}
