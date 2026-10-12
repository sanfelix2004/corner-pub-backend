package com.corner.pub.controller;

import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.response.EventRegistrationResponse;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.repository.EventRegistrationRepository;
import com.corner.pub.service.EventRegistrationService;
import com.corner.pub.service.EventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final EventRegistrationService registrationService;
    private final EventRegistrationRepository registrationRepository;
    private final EventService eventService;

    public EventController(EventRepository eventRepository,
                           EventRegistrationService registrationService,
                           EventRegistrationRepository registrationRepository,
                           EventService eventService) {
        this.eventRepository = eventRepository;
        this.registrationService = registrationService;
        this.registrationRepository = registrationRepository;
        this.eventService = eventService;
    }
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(eventService.listBookablePublic(date));
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<?> registerForEvent(
            @PathVariable Long eventId,
            @RequestBody EventRegistrationRequest request) {

        try {
            // ora register ritorna direttamente un DTO
            EventRegistrationResponse response = registrationService.register(eventId, request);

            return ResponseEntity.ok(response);

        } catch (CornerPubException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long eventId) {
        try {
            return ResponseEntity.ok(eventService.getById(eventId));
        } catch (CornerPubException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @RequestParam(defaultValue = "3") int limit) {
        List<EventResponse> all = eventService.listBookablePublic(null);
        return ResponseEntity.ok(all.stream().limit(Math.max(1, limit)).collect(Collectors.toList()));
    }

    @DeleteMapping("/{eventId}/unregister/{phone}")
    public ResponseEntity<?> unregisterFromEvent(
            @PathVariable Long eventId,
            @PathVariable String phone) {
        try {
            registrationService.unregisterByPhone(eventId, phone);
            return ResponseEntity.ok(Map.of("message", "Registrazione annullata con successo"));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Errore durante l'annullamento della registrazione"));
        }
    }

    @GetMapping("/attendance")
    public ResponseEntity<?> getAttendance(@RequestParam String token) {
        try {
            return ResponseEntity.ok(registrationService.getAttendanceByToken(token));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/attendance")
    public ResponseEntity<?> saveAttendance(@RequestBody Map<String, Object> body) {
        try {
            String token = String.valueOf(body.getOrDefault("token", "")).trim();
            Object attendingValue = body.get("attending");
            boolean attending = attendingValue instanceof Boolean
                    ? (Boolean) attendingValue
                    : Boolean.parseBoolean(String.valueOf(attendingValue));
            return ResponseEntity.ok(registrationService.saveAttendance(token, attending));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
