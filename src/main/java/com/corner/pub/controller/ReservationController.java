package com.corner.pub.controller;

import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.request.CancelReservationRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.dto.response.CancelReservationResponse;
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

    // POST: crea prenotazione
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@RequestBody ReservationRequest request) {
        ReservationResponse dto = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // DELETE: cancella prenotazione via path
    @DeleteMapping("/delete/{phone}/{date}")
    public ResponseEntity<Void> deleteByParams(
            @PathVariable String phone,
            @PathVariable String date) {
        reservationService.deleteReservation(phone, date);
        return ResponseEntity.ok().build();
    }

    // GET: ottieni singola prenotazione via path
    @GetMapping("/{phone}/{date}")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable String phone,
            @PathVariable String date) {
        ReservationResponse response = reservationService.getReservation(phone, date);
        return ResponseEntity.ok(response);
    }

    // GET: per data
    @GetMapping("/date/{date}")
    public ResponseEntity<List<ReservationResponse>> getByDate(@PathVariable String date) {
        List<ReservationResponse> list = reservationService.getReservationsByDate(date);
        return ResponseEntity.ok(list);
    }

    // GET: per telefono
    @GetMapping("/phone/{phone}")
    public ResponseEntity<List<ReservationResponse>> getByPhone(@PathVariable String phone) {
        List<ReservationResponse> list = reservationService.getReservationsByPhone(phone);
        return ResponseEntity.ok(list);
    }

    // GET: orari disponibili via path
    @GetMapping("/available-times/{date}")
    public ResponseEntity<List<String>> getAvailableTimes(@PathVariable String date) {
        List<String> available = reservationService.getAvailableTimes(date);
        return ResponseEntity.ok(available);
    }

    // POST: solo telefono (richiesta contatto)
    @PostMapping("/notify-phone-only")
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

    // GET: prenotazioni future per telefono
    @GetMapping("/by-phone/{phone}")
    public ResponseEntity<List<ReservationResponse>> getFutureReservations(@PathVariable String phone) {
        List<ReservationResponse> list = reservationService.getFutureReservationsByPhone(phone);
        return ResponseEntity.ok(list);
    }
}
