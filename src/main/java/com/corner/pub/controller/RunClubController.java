package com.corner.pub.controller;

import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.service.EventRegistrationService;
import com.corner.pub.service.RunClubService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/run-club")
public class RunClubController {

    private final RunClubService runClubService;
    private final EventRegistrationService registrationService;

    public RunClubController(RunClubService runClubService, EventRegistrationService registrationService) {
        this.runClubService = runClubService;
        this.registrationService = registrationService;
    }

    @GetMapping("/events")
    public List<EventResponse> events() {
        return runClubService.listPublic();
    }

    @PostMapping("/events/{eventId}/register")
    public ResponseEntity<?> register(@PathVariable Long eventId, @RequestBody EventRegistrationRequest request) {
        try {
            return ResponseEntity.ok(registrationService.register(eventId, request));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/tickets/recover")
    public ResponseEntity<?> recover(@RequestBody Map<String, Object> body) {
        try {
            Long eventId = body.get("eventId") == null ? null : Long.valueOf(String.valueOf(body.get("eventId")));
            String phone = body.get("phone") == null ? "" : String.valueOf(body.get("phone"));
            if (eventId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Scegli l'uscita."));
            }
            return ResponseEntity.ok(runClubService.recover(eventId, phone));
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uscita non valida."));
        }
    }

    @GetMapping("/tickets/{code}/pdf")
    public ResponseEntity<?> pdf(@PathVariable String code) {
        try {
            byte[] bytes = runClubService.pdf(code);
            String safe = code.replaceAll("[^A-Za-z0-9-]", "");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"corner-run-club-" + safe + ".pdf\"")
                    .body(bytes);
        } catch (CornerPubException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
