package com.ticketly.mseventseating.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class UnauthorizedExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String errorMessage = "User is not authorized";
        
        // When
        UnauthorizedException exception = new UnauthorizedException(errorMessage);
        
        // Then
        assertEquals(errorMessage, exception.getMessage());
    }
    
    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String errorMessage = "User is not authorized";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        UnauthorizedException exception = new UnauthorizedException(errorMessage, cause);
        
        // Then
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    void shouldHaveCorrectResponseStatus() {
        // Given
        ResponseStatus annotation = UnauthorizedException.class.getAnnotation(ResponseStatus.class);
        
        // Then
        assertNotNull(annotation, "UnauthorizedException should have @ResponseStatus annotation");
        assertEquals(HttpStatus.FORBIDDEN, annotation.value(), "UnauthorizedException should map to HTTP 403 FORBIDDEN");
    }
}