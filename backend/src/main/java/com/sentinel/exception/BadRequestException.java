package com.sentinel.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public final class BadRequestException extends ApplicationException {
    private String provider;

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
