package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.PromotionRequest;
import com.corner.pub.dto.response.PromotionResponse;
import com.corner.pub.model.Promotion;
import com.corner.pub.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/promotions")
public class AdminPromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping
    public List<PromotionResponse> getAll() {
        return promotionService.getAllPromotions().stream()
                .map(promotionService::toResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public PromotionResponse update(@PathVariable Long id, @RequestBody PromotionRequest promotionRequest) {
        Promotion updated = promotionService.update(id, promotionRequest);
        return promotionService.toResponse(updated);
    }

    @PostMapping
    public PromotionResponse create(@RequestBody PromotionRequest promotionRequest) {
        Promotion saved = promotionService.create(promotionRequest);
        return promotionService.toResponse(saved);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        promotionService.delete(id);
    }

    @GetMapping("/{id}")
    public PromotionResponse getById(@PathVariable Long id) {
        Promotion promo = promotionService.getById(id);
        return promotionService.toResponse(promo);
    }

    @PutMapping("/{id}/disattiva")
    public PromotionResponse disattiva(@PathVariable Long id) {
        Promotion promozione = promotionService.disattiva(id);
        return promotionService.toResponse(promozione);
    }

    @PutMapping("/{id}/attiva")
    public PromotionResponse riattiva(@PathVariable Long id) {
        Promotion promozione = promotionService.riattiva(id);
        return promotionService.toResponse(promozione);
    }


}
