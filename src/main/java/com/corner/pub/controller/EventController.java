package com.corner.pub.controller;

import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.response.EventRegistrationResponse;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.model.Event;
import com.corner.pub.model.EventRegistration;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.repository.EventRegistrationRepository;
import com.corner.pub.service.EventRegistrationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final EventRegistrationService registrationService;
    private final EventRegistrationRepository registrationRepository;

    public EventController(EventRepository eventRepository,
                           EventRegistrationService registrationService,
                           EventRegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationService = registrationService;
        this.registrationRepository = registrationRepository;
    }
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Event> events;
        if (date != null) {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            events = eventRepository.findByDataBetween(startOfDay, endOfDay);
        } else {
            events = eventRepository.findByDataAfterOrderByDataAsc(LocalDateTime.now());
        }

        List<EventResponse> response = events.stream()
                .map(event -> {
                    long totalePartecipanti = registrationService.getTotalePartecipantiByEventId(event.getId());
                    return new EventResponse(event, totalePartecipanti);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<?> registerForEvent(
            @PathVariable Long eventId,
            @RequestBody EventRegistrationRequest request) { // <-- INGRESSO

        try {
            EventRegistration registration = registrationService.register(eventId, request);

            EventRegistrationResponse response = new EventRegistrationResponse(registration);

            return ResponseEntity.ok(response); // <-- USCITA

        } catch (CornerPubException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Errore imprevisto"));
        }
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    long totalePartecipanti = registrationService.getTotalePartecipantiByEventId(eventId);
                    return ResponseEntity.ok(new EventResponse(event, totalePartecipanti));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @RequestParam(defaultValue = "3") int limit) {
        List<Event> events = eventRepository
                .findTopByDataAfterOrderByDataAsc(LocalDateTime.now(), limit);

        List<EventResponse> response = events.stream()
                .map(event -> {
                    long totalePartecipanti = registrationService.getTotalePartecipantiByEventId(event.getId());
                    return new EventResponse(event, totalePartecipanti);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
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


}
