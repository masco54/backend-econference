package com.econference.auth.application.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Email not verified");
    }
}
