package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.SpecialeRequest;
import com.corner.pub.dto.response.SpecialeResponse;
import com.corner.pub.service.SpecialeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/speciali")
public class AdminSpecialeController {

    private final SpecialeService specialeService;

    public AdminSpecialeController(SpecialeService specialeService) {
        this.specialeService = specialeService;
    }

    @GetMapping
    public ResponseEntity<List<SpecialeResponse>> getAll() {
        return ResponseEntity.ok(specialeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecialeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(specialeService.getById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SpecialeResponse> create(
            @ModelAttribute @Valid SpecialeRequest request,
            @RequestPart(value = "immagine", required = false) MultipartFile immagine
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specialeService.create(request, immagine));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SpecialeResponse> update(
            @PathVariable Long id,
            @ModelAttribute @Valid SpecialeRequest request,
            @RequestPart(value = "immagine", required = false) MultipartFile immagine
    ) {
        return ResponseEntity.ok(specialeService.update(id, request, immagine));
    }

    @PutMapping("/{id}/attiva")
    public ResponseEntity<SpecialeResponse> attiva(@PathVariable Long id) {
        return ResponseEntity.ok(specialeService.setAttivo(id, true));
    }

    @PutMapping("/{id}/disattiva")
    public ResponseEntity<SpecialeResponse> disattiva(@PathVariable Long id) {
        return ResponseEntity.ok(specialeService.setAttivo(id, false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        specialeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
