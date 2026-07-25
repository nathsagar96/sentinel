package com.sentinel.exception;

import org.springframework.http.HttpStatus;

public final class BadRequestException extends ApplicationException {
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
