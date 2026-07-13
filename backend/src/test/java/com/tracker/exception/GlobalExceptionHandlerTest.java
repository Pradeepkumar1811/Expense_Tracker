package com.tracker.exception;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleEmailAlreadyExists_returns409() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("Email already exists: test@example.com");

        ResponseEntity<ErrorResponse> response = handler.handleEmailAlreadyExists(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Email already exists: test@example.com", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleCategoryDuplicate_returns409() {
        CategoryDuplicateException ex = new CategoryDuplicateException("Category 'Food' already exists for user 1");

        ResponseEntity<ErrorResponse> response = handler.handleCategoryDuplicate(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
    }

    @Test
    void handleDuplicateBudget_returns409() {
        DuplicateBudgetException ex = new DuplicateBudgetException("Budget already exists for month 3/2024");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateBudget(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
    }

    @Test
    void handleInvalidCredentials_returns401() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid email or password");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("Invalid email or password", response.getBody().message());
    }

    @Test
    void handleJwtAuthentication_returns401() {
        JwtAuthenticationException ex = new JwtAuthenticationException("Token has expired");

        ResponseEntity<ErrorResponse> response = handler.handleJwtAuthentication(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("Token has expired", response.getBody().message());
    }

    @Test
    void handleResourceNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Transaction not found with id: 42");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Transaction not found with id: 42", response.getBody().message());
    }

    @Test
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("You do not have permission to access this resource");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
        assertEquals("Forbidden", response.getBody().error());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleValidationErrors_returns400WithFieldErrors() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must be a valid email"));
        bindingResult.addError(new FieldError("request", "password", "size must be between 8 and 100"));

        MethodParameter param = new MethodParameter(
                this.getClass().getDeclaredMethod("handleValidationErrors_returns400WithFieldErrors"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Bad Request", body.get("error"));
        assertEquals("Validation failed", body.get("message"));

        Map<String, String> fieldErrors = (Map<String, String>) body.get("fieldErrors");
        assertNotNull(fieldErrors);
        assertEquals("must be a valid email", fieldErrors.get("email"));
        assertEquals("size must be between 8 and 100", fieldErrors.get("password"));
    }
}
