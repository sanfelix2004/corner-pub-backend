package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.EventRequest;
import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.request.EventTableAssignmentRequest;
import com.corner.pub.dto.response.EventRegistrationResponse;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.dto.response.UserResponse;
import com.corner.pub.model.Event;
import com.corner.pub.model.EventRegistration;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.service.EventRegistrationService;
import com.corner.pub.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventRepository eventRepository;
    private final EventRegistrationService registrationService;
    private final EventService eventService;

    public AdminEventController(EventRepository eventRepository,
                                EventRegistrationService registrationService,
                                EventService eventService) {
        this.eventRepository = eventRepository;
        this.registrationService = registrationService;
        this.eventService = eventService;
    }

    @PatchMapping("/registrations/{id}/table")
    public ResponseEntity<EventRegistrationResponse> updateTable(
            @PathVariable Long id,
            @RequestBody EventTableAssignmentRequest request
    ) {
        return ResponseEntity.ok(registrationService.assignTable(id, request.getTableNumber()));
    }


    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<Event> events = eventRepository.findAllByOrderByDataAsc();
        List<EventResponse> response = events.stream()
                .map(event -> new EventResponse(event,
                        registrationService.getTotalePartecipantiByEventId(event.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> ResponseEntity.ok(
                        new EventResponse(event,
                                registrationService.getTotalePartecipantiByEventId(eventId))))
                .orElse(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{eventId}/unregister/{userId}")
    public ResponseEntity<Void> unregisterFromEvent(@PathVariable Long eventId,
                                                    @PathVariable Long userId) {
        registrationService.unregister(eventId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventResponse> createEvent(
            @ModelAttribute @Valid EventRequest request,
            @RequestPart(value = "poster", required = false) MultipartFile poster
    ) {
        EventResponse response = eventService.createEvent(request, poster);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @ModelAttribute @Valid EventRequest request,
            @RequestPart(value = "poster", required = false) MultipartFile poster
    ) {
        EventResponse response = eventService.updateEvent(eventId, request, poster);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<Void> registerToEvent(
            @PathVariable Long eventId,
            @RequestBody @Valid EventRegistrationRequest request) {
        registrationService.register(eventId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/attendees")
    public ResponseEntity<List<EventRegistrationResponse>> getAttendees(@PathVariable Long eventId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByEventId(eventId));
    }

    @GetMapping("/registrations")
    public ResponseEntity<List<EventRegistrationResponse>> getAllRegistrations() {
        return ResponseEntity.ok(registrationService.getAllRegistrations());
    }

}