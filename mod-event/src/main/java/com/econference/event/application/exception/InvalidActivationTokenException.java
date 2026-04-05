package com.econference.event.application.exception;

public class InvalidActivationTokenException extends RuntimeException {

    public InvalidActivationTokenException(String message) {
        super(message);
    }
}