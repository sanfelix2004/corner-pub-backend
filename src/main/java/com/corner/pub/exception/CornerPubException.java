package com.corner.pub.exception;

/** Radice di tutte le eccezioni Corner Pub. */
public abstract class CornerPubException extends RuntimeException {
    public CornerPubException(String message) { super(message); }
}
