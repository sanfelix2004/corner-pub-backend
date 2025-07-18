package com.corner.pub.controller;

import com.corner.pub.dto.response.InEvidenzaResponse;
import com.corner.pub.service.InEvidenzaService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@CrossOrigin(
        origins = "https://corner-frontend.onrender.com",
        allowCredentials = "true"
)
@RestController
@RequestMapping("/api/in_evidenza")
public class InEvidenzaController {

    private final InEvidenzaService inEvidenzaService;

    public InEvidenzaController(InEvidenzaService inEvidenzaService) {
        this.inEvidenzaService = inEvidenzaService;
    }

    /**
     * Restituisce tutti i prodotti in evidenza (solo lettura, per il front-end pubblico).
     */
    @GetMapping
    public List<InEvidenzaResponse> getAllHighlights() {
        return inEvidenzaService.listAll();
    }
}