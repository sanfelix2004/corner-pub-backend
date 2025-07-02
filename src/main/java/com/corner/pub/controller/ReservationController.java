package com.corner.pub.controller;

import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.request.CancelReservationRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.dto.response.CancelReservationResponse;
import com.corner.pub.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @Autowired
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createReservation(request));
    }

    @DeleteMapping
    public ResponseEntity<CancelReservationResponse> cancel(@RequestBody CancelReservationRequest req) {
        reservationService.deleteReservation(req.getPhone(), req.getDate());
        CancelReservationResponse resp = new CancelReservationResponse(true,
                "Prenotazione cancellata con successo.");
        return ResponseEntity.ok(resp);
    }
}
