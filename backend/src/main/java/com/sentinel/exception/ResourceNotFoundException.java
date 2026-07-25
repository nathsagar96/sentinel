package com.sentinel.exception;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resource, Object id) {
        super("%s not found with id: %s".formatted(resource, id), HttpStatus.NOT_FOUND);
    }
}
