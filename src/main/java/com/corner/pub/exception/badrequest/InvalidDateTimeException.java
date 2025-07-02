package com.corner.pub.exception.badrequest;

public class InvalidDateTimeException extends BadRequestException {
    public InvalidDateTimeException(String dateTime) {
        super("Data/Ora non valida: " + dateTime);
    }
}
