package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationService reservationService;

    // ✅ Leggi tutte le prenotazioni
    @GetMapping
    public List<ReservationResponse> getAllReservations() {
        return reservationService.getAllReservations();
    }

    // ✅ Leggi una prenotazione specifica (opzionale)
    @GetMapping("/{id}")
    public ReservationResponse getReservationById(@PathVariable Long id) {
        return reservationService.getReservationById(id);
    }

    // ✅ Crea una nuova prenotazione
    @PostMapping
    public ReservationResponse createReservation(@RequestBody ReservationRequest request) {
        return reservationService.createReservation(request);
    }

    // ✅ Modifica una prenotazione esistente
    @PutMapping("/{id}")
    public ReservationResponse updateReservation(@PathVariable Long id, @RequestBody ReservationRequest request) {
        return reservationService.updateReservation(id, request);
    }

    // ✅ Cancella una prenotazione (con ID o data+telefono)
    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        reservationService.deleteById(id);
    }

    // ❗ Alternativa: cancella per telefono + data (già esistente)
    @DeleteMapping
    public void deleteByPhoneAndDate(@RequestParam String phone, @RequestParam String date) {
        reservationService.deleteReservationByPhoneAndDate(phone, date);
    }
}
