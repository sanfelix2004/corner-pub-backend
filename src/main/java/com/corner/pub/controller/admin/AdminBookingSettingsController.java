package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.BookingSettingsRequest;
import com.corner.pub.dto.response.BookingSettingsResponse;
import com.corner.pub.service.BookingSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/booking-settings")
public class AdminBookingSettingsController {

    private final BookingSettingsService bookingSettingsService;

    public AdminBookingSettingsController(BookingSettingsService bookingSettingsService) {
        this.bookingSettingsService = bookingSettingsService;
    }

    @GetMapping
    public ResponseEntity<BookingSettingsResponse> get() {
        return ResponseEntity.ok(bookingSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<BookingSettingsResponse> update(@RequestBody BookingSettingsRequest request) {
        return ResponseEntity.ok(bookingSettingsService.update(request));
    }
}
