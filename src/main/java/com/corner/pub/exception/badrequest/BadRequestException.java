package com.corner.pub.exception.badrequest;

import com.corner.pub.exception.CornerPubException;

/** 400 – dati errati / formati non validi. */
public class BadRequestException extends CornerPubException {
    public BadRequestException(String message) { super(message); }
}
