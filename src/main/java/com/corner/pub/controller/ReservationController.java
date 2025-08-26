package com.corner.pub.controller;

import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.service.EventRegistrationService;
import com.corner.pub.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final EventRegistrationService eventRegistrationService;
    private final ReservationService reservationService;

    @Autowired
    public ReservationController(EventRegistrationService eventRegistrationService, ReservationService reservationService) {
        this.eventRegistrationService = eventRegistrationService;
        this.reservationService = reservationService;
    }

    // ✅ POST: crea prenotazione
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@RequestBody ReservationRequest request) {
        ReservationResponse dto = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ✅ GET: tutte le prenotazioni di un utente (tavolo + eventi)
    @GetMapping("/user/{phone}")
    public ResponseEntity<List<ReservationResponse>> getUserReservations(@PathVariable String phone) {
        return ResponseEntity.ok(reservationService.getAllUserReservations(phone));
    }

    // ✅ GET: prenotazioni future di un utente
    @GetMapping("/future/{phone}")
    public ResponseEntity<List<ReservationResponse>> getFuture(@PathVariable String phone) {
        return ResponseEntity.ok(reservationService.getFutureReservationsByPhone(phone));
    }

    // ✅ GET: prenotazione singola (phone + date)
    @GetMapping("/lookup/{phone}/{date}")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable String phone,
            @PathVariable String date) {
        ReservationResponse response = reservationService.getReservation(phone, date);
        return ResponseEntity.ok(response);
    }

    // ✅ GET: prenotazioni per phone (solo prenotazioni tavolo, senza eventi)
    @GetMapping("/byPhone/{phone}")
    public ResponseEntity<List<ReservationResponse>> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(reservationService.getReservationsByPhone(phone));
    }

    // ✅ DELETE prenotazione
    @DeleteMapping("/{phone}/{date}")
    public ResponseEntity<Void> delete(
            @PathVariable String phone,
            @PathVariable String date) {
        reservationService.deleteReservationByPhoneAndDate(phone, date);
        return ResponseEntity.ok().build();
    }

    // ✅ GET: orari disponibili
    @GetMapping("/available/{date}")
    public ResponseEntity<List<String>> getAvailableTimes(@PathVariable String date) {
        return ResponseEntity.ok(reservationService.getAvailableTimes(date));
    }

    // ✅ GET: tutte le registrazioni evento
    @GetMapping("/events")
    public ResponseEntity<?> getEventRegistrations() {
        return ResponseEntity.ok(eventRegistrationService.getAllRegistrations());
    }
}
