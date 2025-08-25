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
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.corner.pub.dto.request.PromotionRequest;
import com.corner.pub.dto.request.PromotionMenuItemRequest;
import com.corner.pub.model.MenuItem;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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

    @Transactional(readOnly = true)
    public List<PromotionResponse> getActivePromotionResponses() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));

        // Usa SEMPRE il metodo fetchato oppure (vedi punto 2) quello con @EntityGraph
        List<Promotion> promos = promotionRepository.findActiveValidFetched(today);

        // FORZA INIZIALIZZAZIONE prima della chiusura TX
        for (Promotion p : promos) {
            Hibernate.initialize(p.getItems());
            if (p.getItems() != null) {
                for (var i : p.getItems()) {
                    if (i.getMenuItem() != null) {
                        Hibernate.initialize(i.getMenuItem());
                    }
                }
            }
        }

        // MAPPATURA *dentro* la transazione (no stream, nessuna lambda rimandata)
        List<PromotionResponse> out = new java.util.ArrayList<>(promos.size());
        for (Promotion p : promos) {
            out.add(toResponse(p));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> getAllPromotionResponses() {
        List<Promotion> promos = promotionRepository.findAllFetched();

        for (Promotion p : promos) {
            Hibernate.initialize(p.getItems());
            if (p.getItems() != null) {
                for (var i : p.getItems()) {
                    if (i.getMenuItem() != null) {
                        Hibernate.initialize(i.getMenuItem());
                    }
                }
            }
        }

        List<PromotionResponse> out = new java.util.ArrayList<>(promos.size());
        for (Promotion p : promos) {
            out.add(toResponse(p));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Promotion getByIdFetched(Long id) {
        Promotion p = promotionRepository.findByIdFetched(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promozione non trovata con ID: " + id));

        Hibernate.initialize(p.getItems());
        if (p.getItems() != null) {
            for (var i : p.getItems()) {
                if (i.getMenuItem() != null) {
                    Hibernate.initialize(i.getMenuItem());
                }
            }
        }
        return p;
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

    @Transactional
    public PromotionResponse disattivaAndMap(Long id) {
        Promotion p = promotionRepository.findByIdFetched(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promozione non trovata con ID: " + id));
        p.setAttiva(false);
        // items sono già inizializzati e restiamo nella TX
        return toResponse(p);
    }

    @Transactional
    public PromotionResponse riattivaAndMap(Long id) {
        Promotion p = promotionRepository.findByIdFetched(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promozione non trovata con ID: " + id));
        p.setAttiva(true);
        return toResponse(p);
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
                .filter(item -> item.getMenuItem() != null) // evita NPE su item orfani
                .map(item -> {
                    MenuItem menuItem = item.getMenuItem();

                    // Converto in DTO con gestione sicura per l'immagine
                    MenuItemResponse menuItemResponse = mapToMenuItemResponse(menuItem);

                    return new PromotionItemDetail(
                            menuItemResponse,
                            item.getScontoPercentuale() != null
                                    ? item.getScontoPercentuale().doubleValue()
                                    : 0.0
                    );
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
        response.setVisibile(item.isVisibile());

        try {
            // Se Cloudinary è configurato bene genera URL, altrimenti fallback
            String imageUrl = cloudinary.url()
                    .secure(true)
                    .version(System.currentTimeMillis() / 1000)
                    .generate("prodotti/" + item.getId());

            response.setImageUrl(imageUrl);
        } catch (Exception e) {
            response.setImageUrl("/images/default.png");
        }

        return response;
    }


}
