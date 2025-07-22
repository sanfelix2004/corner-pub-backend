package com.corner.pub.controller;

import com.corner.pub.model.Promotion;
import com.corner.pub.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping("/attive")
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        List<Promotion> attive = promotionService.getAllPromotions().stream()
                .filter(Promotion::isAttiva)
                .collect(Collectors.toList());
        return ResponseEntity.ok(attive);
    }
}
