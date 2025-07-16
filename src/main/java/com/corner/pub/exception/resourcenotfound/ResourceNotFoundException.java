package com.corner.pub.exception.resourcenotfound;

import com.corner.pub.exception.CornerPubException;

/** 404 – risorsa mancante. */
public class ResourceNotFoundException extends CornerPubException {
    public ResourceNotFoundException(String message) { super(message); }
}
