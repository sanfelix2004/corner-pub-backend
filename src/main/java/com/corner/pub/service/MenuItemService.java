package com.corner.pub.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.corner.pub.dto.request.AllergenSelection;
import com.corner.pub.dto.request.MenuItemRequest;
import com.corner.pub.dto.response.AllergenResponse;
import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.exception.conflictexception.MenuItemDuplicateException;
import com.corner.pub.exception.resourcenotfound.MenuItemNotFoundException;
import com.corner.pub.model.Allergen;
import com.corner.pub.model.AllergenStatus;
import com.corner.pub.model.Category;
import com.corner.pub.model.MenuItem;
import com.corner.pub.model.MenuItemAllergen;
import com.corner.pub.repository.AllergenRepository;
import com.corner.pub.repository.CategoryRepository;
import com.corner.pub.repository.MenuItemAllergenRepository;
import com.corner.pub.repository.MenuItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemAllergenRepository menuItemAllergenRepository;
    private final AllergenRepository allergenRepository;
    private final CategoryRepository categoryRepository;
    private final Cloudinary cloudinary;

    private static final Logger log = LoggerFactory.getLogger(MenuItemService.class);

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Autowired
    public MenuItemService(MenuItemRepository menuItemRepository,
                           MenuItemAllergenRepository menuItemAllergenRepository,
                           AllergenRepository allergenRepository,
                           CategoryRepository categoryRepository,
                           Cloudinary cloudinary) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemAllergenRepository = menuItemAllergenRepository;
        this.allergenRepository = allergenRepository;
        this.categoryRepository = categoryRepository;
        this.cloudinary = cloudinary;
    }

    /** 🔹 Restituisce l’intero menu (visibili + nascosti). */
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** 🔹 Aggiunge un nuovo piatto e carica l’immagine su Cloudinary. */
    @Transactional
    public MenuItemResponse addMenuItem(MenuItemRequest request, MultipartFile image) {
        String titolo = safeTrim(request.getTitolo());
        Category category = resolveCategory(request);

        boolean exists = menuItemRepository.findAll().stream()
                .anyMatch(i ->
                        equalsIgnoreCaseSafe(i.getTitolo(), titolo) &&
                                equalsIgnoreCaseSafe(categoryNameOf(i.getCategory()), categoryNameOf(category))
                );
        if (exists) throw new MenuItemDuplicateException(titolo);

        MenuItem item = new MenuItem();
        item.setCategory(category);
        item.setTitolo(titolo);
        item.setDescrizione(request.getDescrizione());
        item.setPrezzo(request.getPrezzo());
        item.setVisibile(true);

        MenuItem saved = menuItemRepository.save(item);

        applyAllergens(saved, request.getAllergens());

        // Upload immagine su Cloudinary
        if (image != null && !image.isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        image.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "prodotti/",
                                "public_id", String.valueOf(saved.getId()),
                                "overwrite", true,
                                "invalidate", true,
                                "resource_type", "image"
                        )
                );
                String imageUrl = (String) uploadResult.get("secure_url");
                saved.setImageUrl(imageUrl);
                menuItemRepository.save(saved);
            } catch (Exception e) {
                log.warn("Cloudinary upload failed for menuItem id={}: {}", saved.getId(), e.getMessage());
            }
        }

        menuItemRepository.saveAndFlush(saved);
        saved = menuItemRepository.findById(saved.getId()).orElse(saved);
        return mapToResponse(saved);
    }

    /** 🔹 Restituisce un piatto per ID. */
    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));
        return mapToResponse(item);
    }

    /** 🔹 Modifica un piatto esistente e aggiorna immagine/allergeni se presenti. */
    @Transactional
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request, MultipartFile image) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));

        String nuovoTitolo = safeTrim(request.getTitolo());
        Category nuovaCategoria = resolveCategory(request);

        item.setCategory(nuovaCategoria);
        item.setTitolo(nuovoTitolo);
        item.setDescrizione(request.getDescrizione());
        item.setPrezzo(request.getPrezzo());

        applyAllergens(item, request.getAllergens());

        // Upload nuova immagine
        if (image != null && !image.isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        image.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "prodotti/",
                                "public_id", String.valueOf(item.getId()),
                                "overwrite", true,
                                "invalidate", true,
                                "resource_type", "image"
                        )
                );
                String imageUrl = (String) uploadResult.get("secure_url");
                item.setImageUrl(imageUrl);
            } catch (Exception e) {
                log.warn("Cloudinary upload failed on update for menuItem id={}: {}", item.getId(), e.getMessage());
            }
        }

        MenuItem updated = menuItemRepository.save(item);
        return mapToResponse(updated);
    }

    /** 🔹 Elimina un piatto per ID e l'immagine associata su Cloudinary. */
    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));

        menuItemAllergenRepository.deleteByMenuItem_Id(id);

        try {
            String publicId = "prodotti/" + id;
            Map<String, Object> opts = ObjectUtils.asMap(
                    "resource_type", "image",
                    "type", "upload",
                    "invalidate", true
            );
            cloudinary.uploader().destroy(publicId, opts);
            cloudinary.api().deleteResourcesByPrefix(publicId, opts);
        } catch (Exception e) {
            log.warn("Cloudinary delete failed for menuItem id={}: {}", id, e.getMessage());
        }

        menuItemRepository.deleteById(id);
    }

    /** 🔹 Inverte la visibilità (true/false). */
    @Transactional
    public MenuItemResponse toggleVisibility(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));
        item.setVisibile(!item.isVisibile());
        MenuItem updated = menuItemRepository.save(item);
        return mapToResponse(updated);
    }

    /** 🔹 Solo i piatti visibili. */
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getVisibleMenuItems() {
        return menuItemRepository.findAll().stream()
                .filter(MenuItem::isVisibile)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /* ================================================================
       🔧 HELPER METHODS
    ================================================================= */

    /** Mappa MenuItem → Response (inclusi allergeni e icone). */
    private MenuItemResponse mapToResponse(MenuItem item) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(item.getId());
        response.setCategoryName(categoryNameOf(item.getCategory()));
        response.setTitolo(item.getTitolo());
        response.setDescrizione(item.getDescrizione());
        response.setPrezzo(item.getPrezzo());
        response.setVisibile(item.isVisibile());
        response.setImageUrl(item.getImageUrl());

        List<MenuItemAllergen> links = (item.getId() == null)
                ? Collections.emptyList()
                : menuItemAllergenRepository.findByMenuItem_Id(item.getId());

        List<AllergenResponse> ars = new ArrayList<>();
        for (MenuItemAllergen link : links) {
            if (link == null) continue;
            Allergen a = link.getAllergen();
            if (a == null) continue;

            AllergenStatus st = link.getStatus() == null ? AllergenStatus.CONTAINS : link.getStatus();
            String suffix = (st == AllergenStatus.MAY_CONTAIN) ? "__MAY" : "__CONTAINS";

            String base = (a.getIconBase() == null || a.getIconBase().isBlank())
                    ? a.getCode().toLowerCase(Locale.ITALY)
                    : a.getIconBase();

            String cn = (cloudName == null || cloudName.isBlank()) ? "demo" : cloudName;
            String iconUrl = "https://res.cloudinary.com/" + cn + "/image/upload/" + base + suffix + ".svg";

            AllergenResponse ar = new AllergenResponse();
            ar.setCode(a.getCode());
            ar.setLabel(a.getLabel());
            ar.setStatus(st.name());
            ar.setIconUrl(iconUrl);
            ars.add(ar);
        }

        response.setAllergens(ars);
        return response;
    }

    /** 🔧 Applica selezioni allergeni al piatto (replace completo). */
    @Transactional
    protected void applyAllergens(MenuItem item, List<AllergenSelection> selections) {
        if (item.getId() != null) {
            menuItemAllergenRepository.deleteByMenuItem_Id(item.getId());
        }
        if (selections == null || selections.isEmpty()) return;

        for (AllergenSelection sel : selections) {
            if (sel == null || sel.getCode() == null) continue;

            Allergen allergen = allergenRepository.findByCode(sel.getCode().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Allergen code not found: " + sel.getCode()));

            AllergenStatus status = (sel.getStatus() != null)
                    ? AllergenStatus.valueOf(sel.getStatus().toUpperCase())
                    : AllergenStatus.CONTAINS;

            if (status == AllergenStatus.FREE) continue;

            MenuItemAllergen link = new MenuItemAllergen(item, allergen, status);
            menuItemAllergenRepository.save(link);
        }
    }

    /** 🔧 Risolve o crea la categoria richiesta. */
    private Category resolveCategory(MenuItemRequest request) {
        if (request.getCategoryId() != null) {
            return categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata: id=" + request.getCategoryId()));
        }

        String name = safeTrim(request.getCategoryName());
        if (name == null || name.isBlank()) {
            return null;
        }

        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder().name(name).visible(true).build()
                ));
    }

    /* ================================================================
       🔧 Utility methods
    ================================================================= */

    private String categoryNameOf(Category c) {
        return (c == null) ? null : c.getName();
    }

    private String safeTrim(String s) {
        return (s == null) ? null : s.trim();
    }

    private boolean equalsIgnoreCaseSafe(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private String toImageName(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim()
                .toLowerCase()
                .replace(" ", "_");
    }
}
