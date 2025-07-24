package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.PromotionRequest;
import com.corner.pub.dto.response.PromotionResponse;
import com.corner.pub.model.Promotion;
import com.corner.pub.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/admin/promotions")
public class AdminPromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAll() {
        List<PromotionResponse> responses = promotionService.getAllPromotions().stream()
                .map(promotionService::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> update(@PathVariable Long id, @RequestBody PromotionRequest promotionRequest) {
        Promotion updated = promotionService.update(id, promotionRequest);
        return ResponseEntity.ok(promotionService.toResponse(updated));
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> create(@RequestBody PromotionRequest promotionRequest) {
        Promotion saved = promotionService.create(promotionRequest);
        return ResponseEntity.ok(promotionService.toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponse> getById(@PathVariable Long id) {
        Promotion promo = promotionService.getById(id);
        return ResponseEntity.ok(promotionService.toResponse(promo));
    }

    @PutMapping("/{id}/disattiva")
    public ResponseEntity<PromotionResponse> disattiva(@PathVariable Long id) {
        Promotion promozione = promotionService.disattiva(id);
        return ResponseEntity.ok(promotionService.toResponse(promozione));
    }

    @PutMapping("/{id}/attiva")
    public ResponseEntity<PromotionResponse> riattiva(@PathVariable Long id) {
        Promotion promozione = promotionService.riattiva(id);
        return ResponseEntity.ok(promotionService.toResponse(promozione));
    }


}
