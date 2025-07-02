package com.corner.pub.exception;

/** 403 – accesso vietato (futuro back-office). */
public class ForbiddenException extends CornerPubException {
    public ForbiddenException(String message) { super(message); }
}
