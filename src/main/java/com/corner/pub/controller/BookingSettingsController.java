package com.corner.pub.controller;

import com.corner.pub.dto.response.BookingSettingsResponse;
import com.corner.pub.service.BookingSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint pubblico: il sito legge se le prenotazioni sono chiuse (“siamo pieni”). */
@RestController
@RequestMapping("/api/booking-settings")
public class BookingSettingsController {

    private final BookingSettingsService bookingSettingsService;

    public BookingSettingsController(BookingSettingsService bookingSettingsService) {
        this.bookingSettingsService = bookingSettingsService;
    }

    @GetMapping
    public ResponseEntity<BookingSettingsResponse> get() {
        return ResponseEntity.ok(bookingSettingsService.getSettings());
    }
}
