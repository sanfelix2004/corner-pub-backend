package com.corner.pub.service;

import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.dto.response.PromotionMenuItemResponse;
import com.corner.pub.dto.response.PromotionResponse;
import com.corner.pub.exception.resourcenotfound.ResourceNotFoundException;
import com.corner.pub.model.Promotion;
import com.corner.pub.model.PromotionMenuItem;
import com.corner.pub.repository.MenuItemRepository;
import com.corner.pub.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.corner.pub.dto.request.PromotionRequest;
import com.corner.pub.dto.request.PromotionMenuItemRequest;
import com.corner.pub.model.MenuItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    /**
     * Recupera tutte le promozioni presenti nel database.
     */
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    /**
     * Recupera una promozione per ID, oppure lancia eccezione se non esiste.
     */
    public Promotion getById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promozione non trovata con ID: " + id));
    }

    /**
     * Crea una nuova promozione e collega correttamente ogni PromotionMenuItem.
     */

    public Promotion create(PromotionRequest dto) {
        Promotion promo = new Promotion();
        promo.setNome(dto.getNome());
        promo.setAttiva(dto.isAttiva());
        promo.setDescrizione(dto.getDescrizione());
        promo.setDataInizio(dto.getDataInizio());
        promo.setDataFine(dto.getDataFine());

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PromotionMenuItemRequest itemDto : dto.getItems()) {
                MenuItem menuItem = menuItemRepository.findById(itemDto.getMenuItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("MenuItem non trovato con ID: " + itemDto.getMenuItemId()));

                PromotionMenuItem item = new PromotionMenuItem();
                item.setMenuItem(menuItem);
                item.setScontoPercentuale(BigDecimal.valueOf(itemDto.getScontoPercentuale()));
                item.setPromotion(promo);

                promo.getItems().add(item);
            }
        }

        return promotionRepository.save(promo);
    }


    /**
     * Elimina una promozione se esiste, altrimenti non fa nulla.
     */
    public void delete(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Promozione da eliminare non trovata con ID: " + id);
        }
        promotionRepository.deleteById(id);
    }

    public Promotion update(Long id, PromotionRequest request) {
        Promotion existing = getById(id);
        existing.setNome(request.getNome());
        existing.setDescrizione(request.getDescrizione());
        existing.setDataInizio(request.getDataInizio());
        existing.setDataFine(request.getDataFine());
        existing.setAttiva(request.isAttiva());

        // Mappiamo i PromotionMenuItemRequest → PromotionMenuItem
        List<PromotionMenuItem> menuItems = request.getItems().stream().map(reqItem -> {
            PromotionMenuItem item = new PromotionMenuItem();
            item.setPromotion(existing);
            item.setScontoPercentuale(BigDecimal.valueOf(reqItem.getScontoPercentuale()));

            // Recuperiamo il MenuItem dal DB
            MenuItem menuItem = menuItemRepository.findById(reqItem.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem non trovato con ID: " + reqItem.getMenuItemId()));
            item.setMenuItem(menuItem);

            return item;
        }).toList();

        existing.setItems(menuItems);

        return promotionRepository.save(existing);
    }

    public Promotion riattiva(Long id) {
        Promotion p = getById(id);
        p.setAttiva(true);
        return promotionRepository.save(p);
    }

    public Promotion disattiva(Long id) {
        Promotion p = getById(id);
        p.setAttiva(false);
        return promotionRepository.save(p);
    }

    public PromotionResponse toResponse(Promotion promo) {
        PromotionResponse response = new PromotionResponse();
        response.setId(promo.getId());
        response.setNome(promo.getNome());
        response.setAttiva(promo.isAttiva());
        response.setDataInizio(promo.getDataInizio());
        response.setDataFine(promo.getDataFine());
        response.setDescrizione(promo.getDescrizione());

        List<PromotionMenuItemResponse> itemResponses = promo.getItems().stream().map(item -> {
            PromotionMenuItemResponse itemResp = new PromotionMenuItemResponse();
            itemResp.setScontoPercentuale(item.getScontoPercentuale().doubleValue());

            MenuItemResponse menuItem = new MenuItemResponse();
            menuItem.setId(item.getMenuItem().getId());
            menuItem.setTitolo(item.getMenuItem().getTitolo());
            menuItem.setPrezzo(item.getMenuItem().getPrezzo());
            menuItem.setCategoria(item.getMenuItem().getCategoria());

            itemResp.setMenuItem(menuItem);
            return itemResp;
        }).toList();

        response.setItems(itemResponses);
        return response;
    }

}
