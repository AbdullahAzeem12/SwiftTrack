package com.swifttrack.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final boolean retryable;

    public ApiException(String code, String message, HttpStatus status, boolean retryable) {
        super(message);
        this.code = code;
        this.status = status;
        this.retryable = retryable;
    }

    public ApiException(String code, String message, HttpStatus status) {
        this(code, message, status, false);
    }
}
