package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.InEvidenzaRequest;
import com.corner.pub.dto.response.InEvidenzaResponse;
import com.corner.pub.service.InEvidenzaService;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public void addHighlight(@RequestBody InEvidenzaRequest request) {
        inEvidenzaService.add(request);
    }

    /**
     * Rimuove un prodotto da in_evidenza.
     * DELETE /admin/in_evidenza/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHighlight(@PathVariable Long id) {
        inEvidenzaService.remove(id);
    }

    @GetMapping
    public List<InEvidenzaResponse> listAll() {
        return inEvidenzaService.listAll();
    }
}