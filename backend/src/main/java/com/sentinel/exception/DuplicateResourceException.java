package com.sentinel.exception;

import org.springframework.http.HttpStatus;

public final class DuplicateResourceException extends ApplicationException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
