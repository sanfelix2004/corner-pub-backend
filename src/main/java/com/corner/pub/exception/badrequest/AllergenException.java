package com.corner.pub.exception.badrequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class AllergenException extends RuntimeException {
    public AllergenException(String message) {
        super(message);
    }
}
