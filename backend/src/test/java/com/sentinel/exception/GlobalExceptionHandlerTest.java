package com.sentinel.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

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

    @Test
    void shouldHandleMethodArgumentNotValid() throws Exception {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "signupRequest");
        bindingResult.addError(new FieldError("signupRequest", "email", "Must be a valid email address"));
        bindingResult.addError(new FieldError("signupRequest", "name", "Name is required"));

        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterIndex()).thenReturn(-1);
        when(methodParameter.getExecutable()).thenReturn(Object.class.getMethod("toString"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Object> response = exceptionHandler.handleMethodArgumentNotValid(
                ex, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, mock(WebRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getTitle()).isEqualTo("ValidationError");
        assertThat(problemDetail.getProperties()).containsKey("errors");

        @SuppressWarnings("unchecked")
        Map<String, String> errors =
                (Map<String, String>) problemDetail.getProperties().get("errors");
        assertThat(errors).containsEntry("email", "Must be a valid email address");
        assertThat(errors).containsEntry("name", "Name is required");
    }
}
