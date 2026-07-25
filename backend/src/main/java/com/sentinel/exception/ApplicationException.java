package com.sentinel.exception;

import org.springframework.http.HttpStatus;

public sealed abstract class ApplicationException extends RuntimeException
        permits ResourceNotFoundException, DuplicateResourceException, BadRequestException {

    private final HttpStatus httpStatus;

    protected ApplicationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
