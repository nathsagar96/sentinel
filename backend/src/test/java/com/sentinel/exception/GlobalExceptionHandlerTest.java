package com.sentinel.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", 123L);
        ProblemDetail problem = exceptionHandler.handleApplicationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("ResourceNotFoundException");
        assertThat(problem.getDetail()).isEqualTo("User not found with id: 123");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    void shouldHandleDuplicateResourceException() {
        DuplicateResourceException ex = new DuplicateResourceException("Email already exists");
        ProblemDetail problem = exceptionHandler.handleApplicationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("DuplicateResourceException");
        assertThat(problem.getDetail()).isEqualTo("Email already exists");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    void shouldHandleBadRequestException() {
        BadRequestException ex = new BadRequestException("Invalid input");
        ProblemDetail problem = exceptionHandler.handleApplicationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("BadRequestException");
        assertThat(problem.getDetail()).isEqualTo("Invalid input");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }
}
