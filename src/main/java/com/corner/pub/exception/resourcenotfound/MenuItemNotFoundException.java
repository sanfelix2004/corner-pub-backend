package com.corner.pub.exception.resourcenotfound;

public class MenuItemNotFoundException extends ResourceNotFoundException {
    public MenuItemNotFoundException(Long id) {
        super("Piatto con id " + id + " non trovato");
    }
}
