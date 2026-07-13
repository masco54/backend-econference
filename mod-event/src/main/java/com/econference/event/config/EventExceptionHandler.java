package com.econference.event.config;

import com.econference.event.application.exception.DuplicateEventRequestException;
import com.econference.event.application.exception.InvalidActivationTokenException;
import com.econference.event.application.exception.InvalidEventRequestStateException;
import com.econference.shared.exception.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EventExceptionHandler {

    @ExceptionHandler(DuplicateEventRequestException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRequest(DuplicateEventRequestException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage(), "EVENT_REQUEST_ALREADY_EXISTS"));
    }

    @ExceptionHandler(InvalidEventRequestStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidEventRequestStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), "INVALID_EVENT_REQUEST_STATE"));
    }

    @ExceptionHandler(InvalidActivationTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidActivationToken(InvalidActivationTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), "INVALID_ACTIVATION_TOKEN"));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ex.getMessage(), "FORBIDDEN"));
    }
}