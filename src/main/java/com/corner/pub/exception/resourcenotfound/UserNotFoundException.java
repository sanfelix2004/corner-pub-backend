package com.corner.pub.exception.resourcenotfound;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String phone) {
        super("Utente con telefono " + phone + " non trovato");
    }
}
