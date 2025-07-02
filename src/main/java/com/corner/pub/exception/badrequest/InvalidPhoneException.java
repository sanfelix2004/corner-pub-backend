package com.corner.pub.exception.badrequest;

public class InvalidPhoneException extends BadRequestException {
    public InvalidPhoneException(String phone) {
        super("Telefono non valido: " + phone);
    }
}
