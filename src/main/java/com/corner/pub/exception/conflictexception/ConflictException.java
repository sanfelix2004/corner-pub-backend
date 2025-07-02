package com.corner.pub.exception.conflictexception;

import com.corner.pub.exception.CornerPubException;

/** 409 – conflitto di stato/logica. */
public abstract class ConflictException extends CornerPubException {
    public ConflictException(String message) { super(message); }
}
