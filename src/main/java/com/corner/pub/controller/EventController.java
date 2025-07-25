package com.corner.pub.controller;

import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.model.Event;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.service.EventRegistrationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final EventRegistrationService registrationService;

    public EventController(EventRepository eventRepository,
                           EventRegistrationService registrationService) {
        this.eventRepository = eventRepository;
        this.registrationService = registrationService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Event> events;
        if (date != null) {
            // Filtra per eventi nella data specificata
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            events = eventRepository.findByDataBetween(startOfDay, endOfDay);
        } else {
            // Mostra solo eventi futuri
            events = eventRepository.findByDataAfterOrderByDataAsc(LocalDateTime.now());
        }

        List<EventResponse> response = events.stream()
                .map(event -> {
                    long attendees = registrationService.countByEventId(event.getId());
                    return new EventResponse(event, attendees);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    long attendees = registrationService.countByEventId(eventId);
                    return ResponseEntity.ok(new EventResponse(event, attendees));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<Void> registerToEvent(
            @PathVariable Long eventId,
            @RequestBody EventRegistrationRequest request) {
        registrationService.register(eventId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @RequestParam(defaultValue = "3") int limit) {
        List<Event> events = eventRepository
                .findTopByDataAfterOrderByDataAsc(LocalDateTime.now(), limit);

        List<EventResponse> response = events.stream()
                .map(event -> {
                    long attendees = registrationService.countByEventId(event.getId());
                    return new EventResponse(event, attendees);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}