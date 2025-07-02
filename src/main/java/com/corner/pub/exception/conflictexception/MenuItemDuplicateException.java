package com.corner.pub.exception.conflictexception;

public class MenuItemDuplicateException extends ConflictException {
    public MenuItemDuplicateException(String title) {
        super("Il piatto \"" + title + "\" è già presente nel menù");
    }
}
