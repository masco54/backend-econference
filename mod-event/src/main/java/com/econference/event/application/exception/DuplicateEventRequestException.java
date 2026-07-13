package com.econference.event.application.exception;

public class DuplicateEventRequestException extends RuntimeException {

    public DuplicateEventRequestException(String requesterEmail, String eventName) {
        super("Open event request already exists for email '%s' and event '%s'".formatted(requesterEmail, eventName));
    }
}