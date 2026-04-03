package com.econference.shared.exception;

public record ErrorResponse(String message, String code, Object details) {

    public ErrorResponse(String message, String code) {
        this(message, code, null);
    }
}
