package com.sentinel.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public final class BadRequestException extends ApplicationException {
    private final String provider;

    public BadRequestException(String message) {
        this(message, null);
    }

    public BadRequestException(String message, String provider) {
        super(message, HttpStatus.BAD_REQUEST);
        this.provider = provider;
    }
}
