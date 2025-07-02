package com.corner.pub.controller;

import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.request.CancelReservationRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.dto.response.CancelReservationResponse;
import com.corner.pub.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    // ReservationController → crea con 201
    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @RequestBody ReservationRequest request) {
        ReservationResponse dto = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }


    @DeleteMapping
    public ResponseEntity<CancelReservationResponse> cancel(@RequestBody CancelReservationRequest req) {
        reservationService.deleteReservation(req.getPhone(), req.getDate());
        CancelReservationResponse resp = new CancelReservationResponse(true,
                "Prenotazione cancellata con successo.");
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<ReservationResponse> getReservation(
            @RequestParam String phone,
            @RequestParam String date) {
        ReservationResponse response = reservationService.getReservation(phone, date);
        return ResponseEntity.ok(response);
    }

}
