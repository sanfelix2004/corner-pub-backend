package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.InEvidenzaRequest;
import com.corner.pub.dto.response.InEvidenzaResponse;
import com.corner.pub.service.InEvidenzaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/in_evidenza")
public class AdminInEvidenzaController {

    private final InEvidenzaService inEvidenzaService;

    public AdminInEvidenzaController(InEvidenzaService inEvidenzaService) {
        this.inEvidenzaService = inEvidenzaService;
    }

    /**
     * Aggiunge un nuovo prodotto in evidenza.
     * POST /admin/in_evidenza
     */
    @PostMapping
    public ResponseEntity<Void> addHighlight(@RequestBody InEvidenzaRequest request) {
        inEvidenzaService.add(request);
        return ResponseEntity.status(201).build();
    }

    /**
     * Rimuove un prodotto da in_evidenza.
     * DELETE /admin/in_evidenza/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHighlight(@PathVariable Long id) {
        inEvidenzaService.remove(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<InEvidenzaResponse>> listAll() {
        return ResponseEntity.ok(inEvidenzaService.listAll());
    }
}