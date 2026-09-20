package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.request.TableAssignmentRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.service.MailService;
import com.corner.pub.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationService reservationService;
    private final MailService mailService;

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAllTableReservations() {
        return ResponseEntity.ok(reservationService.getActiveReservations());
    }

    @PostMapping("/mail-test")
    public ResponseEntity<String> sendMailTest() {
        mailService.sendTestEmail();
        return ResponseEntity.ok("Mail di prova inviata");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createTableReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createAdminReservation(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationRequest request
    ) {
        return ResponseEntity.ok(reservationService.updateReservation(id, request));
    }

    // 🔹 Nuovo endpoint solo per assegnare/cambiare tavolo
    @PatchMapping("/{id}/table")
    public ResponseEntity<ReservationResponse> updateTable(
            @PathVariable Long id,
            @RequestBody TableAssignmentRequest request
    ) {
        return ResponseEntity.ok(reservationService.assignTable(id, request.getTableNumber()));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        reservationService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteByPhoneAndDate(
            @RequestParam String phone,
            @RequestParam String date
    ) {
        reservationService.deleteReservationByPhoneAndDate(phone, date);
        return ResponseEntity.ok().build();
    }
}
