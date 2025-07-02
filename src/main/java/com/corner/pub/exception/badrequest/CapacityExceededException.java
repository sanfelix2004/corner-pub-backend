package com.corner.pub.exception.badrequest;

public class CapacityExceededException extends BadRequestException {
    public CapacityExceededException(int requested, int max) {
        super("Capienza superata: richiesti " + requested + " posti, massimo " + max);
    }
}
