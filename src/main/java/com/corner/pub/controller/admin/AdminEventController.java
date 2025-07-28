package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.EventRequest;
import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.response.EventRegistrationResponse;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.dto.response.UserResponse;
import com.corner.pub.model.Event;
import com.corner.pub.model.EventRegistration;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.service.EventRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventRepository eventRepository;
    private final EventRegistrationService registrationService;

    public AdminEventController(EventRepository eventRepository,
                                EventRegistrationService registrationService) {
        this.eventRepository = eventRepository;
        this.registrationService = registrationService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<Event> events = eventRepository.findAllByOrderByDataAsc();
        List<EventResponse> response = events.stream()
                .map(event -> new EventResponse(event, registrationService.countByEventId(event.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@RequestBody @Valid EventRequest request) {
        Event event = new Event();
        event.setTitolo(request.getTitolo());
        event.setDescrizione(request.getDescrizione());
        event.setData(request.getData());
        event.setPostiTotali(request.getPostiTotali());

        Event savedEvent = eventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new EventResponse(savedEvent, 0));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> ResponseEntity.ok(
                        new EventResponse(event, registrationService.countByEventId(eventId))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @RequestBody @Valid EventRequest request) {

        return eventRepository.findById(eventId)
                .map(event -> {
                    event.setTitolo(request.getTitolo());
                    event.setDescrizione(request.getDescrizione());
                    event.setData(request.getData());
                    event.setPostiTotali(request.getPostiTotali());
                    Event updatedEvent = eventRepository.save(event);
                    return ResponseEntity.ok(
                            new EventResponse(updatedEvent,
                                    registrationService.countByEventId(eventId)));
                })
                .orElse(ResponseEntity.notFound().build());
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
        if (!eventRepository.existsById(eventId)) {
            return ResponseEntity.notFound().build();
        }
        registrationService.deleteByEventId(eventId);
        eventRepository.deleteById(eventId);
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