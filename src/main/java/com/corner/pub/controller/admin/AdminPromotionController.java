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
        return ResponseEntity.ok(promotionService.getAllPromotionResponses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponse> getById(@PathVariable Long id) {
        Promotion promo = promotionService.getByIdFetched(id);
        return ResponseEntity.ok(promotionService.toResponse(promo));
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> create(@RequestBody PromotionRequest promotionRequest) {
        Promotion saved = promotionService.create(promotionRequest);
        Promotion fetched = promotionService.getByIdFetched(saved.getId());   // ricarico fetchato
        return ResponseEntity.ok(promotionService.toResponse(fetched));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> update(@PathVariable Long id, @RequestBody PromotionRequest promotionRequest) {
        Promotion updated = promotionService.update(id, promotionRequest);
        Promotion fetched = promotionService.getByIdFetched(updated.getId()); // ricarico fetchato
        return ResponseEntity.ok(promotionService.toResponse(fetched));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/disattiva")
    public ResponseEntity<PromotionResponse> disattiva(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.disattivaAndMap(id));
    }

    @PutMapping("/{id}/attiva")
    public ResponseEntity<PromotionResponse> riattiva(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.riattivaAndMap(id));
    }
}
