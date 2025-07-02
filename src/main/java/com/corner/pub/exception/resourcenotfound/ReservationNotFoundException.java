package com.corner.pub.exception.resourcenotfound;

public class ReservationNotFoundException extends ResourceNotFoundException {
    public ReservationNotFoundException(String phone, String date) {
        super("Prenotazione non trovata per " + phone + " alla data " + date);
    }
}
