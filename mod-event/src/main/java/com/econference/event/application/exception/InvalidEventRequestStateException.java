package com.econference.event.application.exception;

public class InvalidEventRequestStateException extends RuntimeException {

    public InvalidEventRequestStateException(String message) {
        super(message);
    }
}