package com.corner.pub.exception.conflictexception;

public class ReservationAlreadyExistsException extends ConflictException {
    public ReservationAlreadyExistsException(String phone, String date) {
        super("Esiste già una prenotazione per " + phone + " alla data " + date);
    }
}
