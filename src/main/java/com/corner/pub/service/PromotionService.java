package com.corner.pub.service;

import com.cloudinary.Cloudinary;
import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.dto.response.PromotionItemDetail;
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

    @Autowired
    private Cloudinary cloudinary;

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
        response.setDescrizione(promo.getDescrizione());
        response.setDataInizio(promo.getDataInizio());
        response.setDataFine(promo.getDataFine());

        List<PromotionItemDetail> itemDetails = promo.getItems().stream()
                .map(item -> {
                    MenuItem menuItem = menuItemRepository.findById(item.getMenuItem().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Menu item non trovato"));

                    // Converti MenuItem in MenuItemResponse per ottenere l'imageUrl
                    MenuItemResponse menuItemResponse = mapToMenuItemResponse(menuItem);

                    return new PromotionItemDetail(menuItemResponse, item.getScontoPercentuale().doubleValue());
                })
                .collect(Collectors.toList());

        response.setItems(itemDetails);
        return response;
    }

    private MenuItemResponse mapToMenuItemResponse(MenuItem item) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(item.getId());
        response.setTitolo(item.getTitolo());
        response.setCategoria(item.getCategoria());
        response.setPrezzo(item.getPrezzo());

        // Genera URL immagine da Cloudinary
        String imageUrl = cloudinary.url()
                .secure(true)
                .version(System.currentTimeMillis() / 1000)
                .generate("prodotti/" + item.getId());

        response.setImageUrl(imageUrl);
        response.setVisibile(item.isVisibile());
        return response;
    }

}
