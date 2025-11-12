package com.corner.pub.service;

import com.cloudinary.Cloudinary;
import com.corner.pub.dto.request.PromotionMenuItemRequest;
import com.corner.pub.dto.request.PromotionRequest;
import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.dto.response.PromotionItemDetail;
import com.corner.pub.dto.response.PromotionResponse;
import com.corner.pub.exception.resourcenotfound.ResourceNotFoundException;
import com.corner.pub.model.MenuItem;
import com.corner.pub.model.Promotion;
import com.corner.pub.model.PromotionMenuItem;
import com.corner.pub.repository.MenuItemRepository;
import com.corner.pub.repository.PromotionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private Cloudinary cloudinary;

    /* ===========================================================
       🔹 Metodi di lettura
    ============================================================ */

    @Transactional
    public List<PromotionResponse> getAllPromotionResponses() {
        return promotionRepository.findAllFetched().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PromotionResponse> getActivePromotionResponses() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));
        return promotionRepository.findActiveValidFetched(today).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Promotion getByIdFetched(Long id) {
        return promotionRepository.findByIdFetched(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promozione non trovata con ID: " + id));
    }

    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    public Promotion getById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promozione non trovata con ID: " + id));
    }

    /* ===========================================================
       🔹 Creazione / Update / Delete
    ============================================================ */

    @Transactional
    public Promotion create(PromotionRequest dto) {
        Promotion promo = new Promotion();
        promo.setNome(dto.getNome());
        promo.setAttiva(dto.isAttiva());
        promo.setDescrizione(dto.getDescrizione());
        promo.setDataInizio(dto.getDataInizio());
        promo.setDataFine(dto.getDataFine());

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            dto.getItems().forEach(req -> promo.getItems().add(buildPromotionItem(req, promo)));
        }

        return promotionRepository.save(promo);
    }

    @Transactional
    public Promotion update(Long id, PromotionRequest request) {
        Promotion existing = getById(id);

        existing.setNome(request.getNome());
        existing.setDescrizione(request.getDescrizione());
        existing.setDataInizio(request.getDataInizio());
        existing.setDataFine(request.getDataFine());
        existing.setAttiva(request.isAttiva());

        existing.getItems().clear();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            request.getItems().forEach(req -> existing.getItems().add(buildPromotionItem(req, existing)));
        }

        return promotionRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Promotion promo = getById(id);
        promotionRepository.delete(promo);
    }

    @Transactional
    public Promotion riattiva(Long id) {
        Promotion p = getById(id);
        p.setAttiva(true);
        return promotionRepository.save(p);
    }

    @Transactional
    public Promotion disattiva(Long id) {
        Promotion p = getById(id);
        p.setAttiva(false);
        return promotionRepository.save(p);
    }

    /* ===========================================================
       🔹 Mapping
    ============================================================ */

    private PromotionMenuItem buildPromotionItem(PromotionMenuItemRequest req, Promotion promo) {
        MenuItem menuItem = menuItemRepository.findById(req.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem non trovato con ID: " + req.getMenuItemId()));

        PromotionMenuItem item = new PromotionMenuItem();
        item.setPromotion(promo);
        item.setMenuItem(menuItem);
        item.setScontoPercentuale(BigDecimal.valueOf(req.getScontoPercentuale()));
        return item;
    }

    public PromotionResponse toResponse(Promotion promo) {
        PromotionResponse res = new PromotionResponse();
        res.setId(promo.getId());
        res.setNome(promo.getNome());
        res.setDescrizione(promo.getDescrizione());
        res.setAttiva(promo.isAttiva());
        res.setDataInizio(promo.getDataInizio());
        res.setDataFine(promo.getDataFine());

        List<PromotionItemDetail> itemDetails = promo.getItems().stream()
                .filter(i -> i.getMenuItem() != null)
                .map(i -> new PromotionItemDetail(
                        mapToMenuItemResponse(i.getMenuItem()),
                        i.getScontoPercentuale() != null ? i.getScontoPercentuale().doubleValue() : 0.0
                ))
                .collect(Collectors.toList());

        res.setItems(itemDetails);
        return res;
    }

    private MenuItemResponse mapToMenuItemResponse(MenuItem item) {
        MenuItemResponse r = new MenuItemResponse();
        r.setId(item.getId());
        r.setTitolo(item.getTitolo());
        r.setDescrizione(item.getDescrizione());
        r.setPrezzo(item.getPrezzo());
        r.setVisibile(item.isVisibile());
        r.setCategoryName(item.getCategory() != null ? item.getCategory().getName() : null);
        r.setImageUrl(resolveImageUrl(item));
        return r;
    }

    private String resolveImageUrl(MenuItem item) {
        try {
            if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
                return item.getImageUrl(); // usa l'URL già salvato da Cloudinary
            }
            return cloudinary.url()
                    .secure(true)
                    .generate("prodotti/" + item.getId());
        } catch (Exception e) {
            return "/images/default.png";
        }
    }
}
