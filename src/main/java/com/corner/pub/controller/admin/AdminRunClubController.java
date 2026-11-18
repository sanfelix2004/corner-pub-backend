package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.request.RunClubEventRequest;
import com.corner.pub.dto.response.EventRegistrationResponse;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.service.EventRegistrationService;
import com.corner.pub.service.RunClubService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/run-club")
public class AdminRunClubController {

    private final RunClubService runClubService;
    private final EventRegistrationService registrationService;

    public AdminRunClubController(RunClubService runClubService, EventRegistrationService registrationService) {
        this.runClubService = runClubService;
        this.registrationService = registrationService;
    }

    @GetMapping("/events")
    public List<EventResponse> events() {
        return runClubService.listAdmin();
    }

    @PostMapping("/events")
    public ResponseEntity<?> create(@RequestBody RunClubEventRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(runClubService.create(request));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RunClubEventRequest request) {
        try {
            return ResponseEntity.ok(runClubService.update(id, request));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/events/{id}/annulla")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(runClubService.setAnnullata(id, true));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/events/{id}/riapri")
    public ResponseEntity<?> reopen(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(runClubService.setAnnullata(id, false));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            runClubService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/events/{id}/attendees")
    public ResponseEntity<?> attendees(@PathVariable Long id) {
        try {
            List<EventRegistrationResponse> rows = runClubService.attendees(id);
            return ResponseEntity.ok(rows);
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/events/{id}/register")
    public ResponseEntity<?> register(@PathVariable Long id, @RequestBody EventRegistrationRequest request) {
        try {
            return ResponseEntity.ok(registrationService.register(id, request, false));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(runClubService.checkIn(body == null ? null : body.get("code")));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/checkin/undo")
    public ResponseEntity<?> undo(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(runClubService.undoCheckIn(body == null ? null : body.get("code")));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
