package com.corner.pub.exception.resourcenotfound;

public class EventRegistrationNotFoundException extends RuntimeException {
    public EventRegistrationNotFoundException(Long id) {
        super("Registrazione evento non trovata con ID: " + id);
    }

    public EventRegistrationNotFoundException(String message) {
        super(message);
    }
}