package com.corner.pub.controller;

import com.corner.pub.dto.response.SpecialeResponse;
import com.corner.pub.service.SpecialeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/speciali")
public class SpecialeController {

    private final SpecialeService specialeService;

    public SpecialeController(SpecialeService specialeService) {
        this.specialeService = specialeService;
    }

    @GetMapping("/attivi")
    public ResponseEntity<List<SpecialeResponse>> getAttivi() {
        return ResponseEntity.ok(specialeService.getActive());
    }
}
