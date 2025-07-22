package com.corner.pub.controller;

import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.service.EmailService;
import com.corner.pub.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final EmailService emailService;

    @Autowired
    public ReservationController(ReservationService reservationService, EmailService emailService) {
        this.reservationService = reservationService;
        this.emailService = emailService;
    }

    // ✅ POST: crea una prenotazione
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@RequestBody ReservationRequest request) {
        ReservationResponse dto = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ✅ GET: singola prenotazione (ATTENTO: DEVE STARE PRIMA DI /{phone})
    @GetMapping("/{phone}/{date}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable String phone, @PathVariable String date) {
        ReservationResponse response = reservationService.getReservation(phone, date);
        return ResponseEntity.ok(response);
    }

    // ✅ DELETE: cancella prenotazione
    @DeleteMapping("/{phone}/{date}")
    public ResponseEntity<Void> delete(@PathVariable String phone, @PathVariable String date) {
        reservationService.deleteReservation(phone, date);
        return ResponseEntity.ok().build();
    }

    // ✅ GET: tutte le prenotazioni di un numero
    @GetMapping("/{phone}")
    public ResponseEntity<List<ReservationResponse>> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(reservationService.getReservationsByPhone(phone));
    }

    // ✅ GET: future prenotazioni
    @GetMapping("/future/{phone}")
    public ResponseEntity<List<ReservationResponse>> getFuture(@PathVariable String phone) {
        return ResponseEntity.ok(reservationService.getFutureReservationsByPhone(phone));
    }

    // ✅ GET: orari disponibili
    @GetMapping("/available/{date}")
    public ResponseEntity<List<String>> getAvailableTimes(@PathVariable String date) {
        return ResponseEntity.ok(reservationService.getAvailableTimes(date));
    }

    // ✅ POST: richiesta contatto utente (telefono)
    @PostMapping("/notify")
    public ResponseEntity<Void> notifyPhoneOnly(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone != null && !phone.isBlank()) {
            emailService.sendSimpleMessage("staff@corner.pub",
                    "Richiesta contatto utente",
                    "Un utente ha lasciato il numero: " + phone);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
