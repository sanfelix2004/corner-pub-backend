package com.corner.pub.service;

import com.corner.pub.dto.request.AllergenSelection;
import com.corner.pub.dto.request.MenuItemRequest;
import com.corner.pub.dto.response.AllergenResponse;
import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.exception.conflictexception.MenuItemDuplicateException;
import com.corner.pub.exception.resourcenotfound.MenuItemNotFoundException;
import com.corner.pub.model.Allergen;
import com.corner.pub.model.AllergenStatus;
import com.corner.pub.model.MenuItem;
import com.corner.pub.model.MenuItemAllergen;
import com.corner.pub.repository.AllergenRepository;
import com.corner.pub.repository.CategoryRepository;
import com.corner.pub.repository.MenuItemAllergenRepository;
import com.corner.pub.repository.MenuItemRepository;
import com.corner.pub.model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemAllergenRepository menuItemAllergenRepository;
    private final AllergenRepository allergenRepository;
    private final StorageService storageService;

    private static final Logger log = LoggerFactory.getLogger(MenuItemService.class);

    private final CategoryRepository categoryRepository;

    @Autowired
    public MenuItemService(MenuItemRepository menuItemRepository,
            MenuItemAllergenRepository menuItemAllergenRepository,
            AllergenRepository allergenRepository,
            CategoryRepository categoryRepository,
            StorageService storageService) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemAllergenRepository = menuItemAllergenRepository;
        this.allergenRepository = allergenRepository;
        this.categoryRepository = categoryRepository;
        this.storageService = storageService;
    }

    /** Restituisce l’intero menu (visibili + nascosti). */
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        List<MenuItem> items = menuItemRepository.findAll();
        List<String> explicitOrder = List.of("panini", "fritti", "polpette", "pinse", "bevande");

        items.sort((i1, i2) -> {
            String c1 = i1.getCategoria() != null ? i1.getCategoria() : "";
            String c2 = i2.getCategoria() != null ? i2.getCategoria() : "";

            int index1 = explicitOrder.indexOf(c1.toLowerCase());
            int index2 = explicitOrder.indexOf(c2.toLowerCase());

            if (index1 != -1 && index2 != -1) {
                if (index1 != index2)
                    return Integer.compare(index1, index2);
            } else if (index1 != -1) {
                return -1;
            } else if (index2 != -1) {
                return 1;
            } else {
                int catComp = c1.compareToIgnoreCase(c2);
                if (catComp != 0)
                    return catComp;
            }

            // if same category
            Integer s1 = i1.getSortOrder() != null ? i1.getSortOrder() : 999;
            Integer s2 = i2.getSortOrder() != null ? i2.getSortOrder() : 999;
            if (!s1.equals(s2))
                return s1.compareTo(s2);

            if (i1.getTitolo() != null && i2.getTitolo() != null) {
                return i1.getTitolo().compareToIgnoreCase(i2.getTitolo());
            }
            return 0;
        });

        return items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Aggiunge un nuovo piatto e carica l’immagine su disco. */
    @Transactional
    public MenuItemResponse addMenuItem(MenuItemRequest request, MultipartFile image) {
        String categoria = request.getCategoryName().trim();
        String titolo = request.getTitolo().trim();

        boolean exists = menuItemRepository.findAll().stream()
                .anyMatch(i -> i.getCategoria() != null
                        && i.getCategoria().equalsIgnoreCase(categoria)
                        && i.getTitolo().equalsIgnoreCase(titolo));
        if (exists)
            throw new MenuItemDuplicateException(titolo);

        // Find or create category if absolutely needed, but usually we expect it to
        // exist if selected from UI
        // However, user might have old UI or direct API. We will throw error if not
        // found to enforce integrity
        Category cat = categoryRepository.findByName(categoria)
                .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata: " + categoria));

        MenuItem item = new MenuItem();
        item.setCategory(cat); // Set REAL relationship
        item.setTitolo(titolo);
        item.setDescrizione(request.getDescrizione());
        item.setPrezzo(request.getPrezzo());
        item.setVisibile(true);

        // salvo per ottenere l'ID
        MenuItem saved = menuItemRepository.save(item);

        // Allergeni (relazioni)
        applyAllergens(saved, request.getAllergens());

        if (image != null && !image.isEmpty()) {
            try {
                String relative = storageService.store("prodotti", String.valueOf(saved.getId()), image);
                saved.setImageUrl(storageService.publicUrl(relative));
                menuItemRepository.save(saved);
            } catch (Exception e) {
                log.error("Upload immagine fallito per menuItem id={}: {}", saved.getId(), e.getMessage());
                throw new IllegalStateException("Impossibile salvare la foto del piatto", e);
            }
        }

        menuItemRepository.saveAndFlush(saved);
        saved = menuItemRepository.findById(saved.getId()).orElse(saved);
        return mapToResponse(saved);
    }

    /** Restituisce un piatto per ID. */
    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));
        return mapToResponse(item);
    }

    /** Modifica un piatto esistente e aggiorna immagine/allergeni se presenti. */
    @Transactional
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request, MultipartFile image) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));

        // Update Category
        String catName = request.getCategoryName().trim();
        Category cat = categoryRepository.findByName(catName)
                .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata: " + catName));
        item.setCategory(cat);

        item.setTitolo(request.getTitolo().trim());
        item.setDescrizione(request.getDescrizione());
        item.setPrezzo(request.getPrezzo());

        // Allergeni (replace completo in base alla richiesta)
        applyAllergens(item, request.getAllergens());

        if (image != null && !image.isEmpty()) {
            try {
                String relative = storageService.replace(
                        "prodotti", String.valueOf(item.getId()), item.getImageUrl(), image);
                item.setImageUrl(storageService.publicUrl(relative));
            } catch (Exception e) {
                log.error("Upload immagine fallito in update per menuItem id={}: {}", item.getId(), e.getMessage());
                throw new IllegalStateException("Impossibile aggiornare la foto del piatto", e);
            }
        }

        MenuItem updated = menuItemRepository.save(item);
        return mapToResponse(updated);
    }

    /** Elimina un piatto per ID. */
    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));

        // 1) elimina relazioni allergeni
        menuItemAllergenRepository.deleteByMenuItem_Id(id);
        storageService.deleteOwned(item.getImageUrl(), "prodotti", String.valueOf(id));

        // 3) elimina record
        menuItemRepository.deleteById(id);
    }

    /** Inverte la visibilità (true/false). */
    @Transactional
    public MenuItemResponse toggleVisibility(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));
        item.setVisibile(!item.isVisibile());
        MenuItem updated = menuItemRepository.save(item);
        return mapToResponse(updated);
    }

    /** Solo i piatti visibili. */
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getVisibleMenuItems() {
        return menuItemRepository.findAll().stream()
                .filter(MenuItem::isVisibile)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /* ===================== Helpers ===================== */

    /** Mappa MenuItem → Response (inclusi allergeni con icone). */
    private MenuItemResponse mapToResponse(MenuItem item) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(item.getId());
        response.setCategoryName(item.getCategoria());
        response.setTitolo(item.getTitolo());
        response.setDescrizione(item.getDescrizione());
        response.setPrezzo(item.getPrezzo());
        response.setVisibile(item.isVisibile());
        response.setImageUrl(item.getImageUrl());

        // Carica i link da repository (no lazy su item) e costruisci DTO in modo
        // null-safe
        List<MenuItemAllergen> links = (item.getId() == null)
                ? Collections.emptyList()
                : menuItemAllergenRepository.findByMenuItem_Id(item.getId());

        List<AllergenResponse> ars = new ArrayList<>();
        for (MenuItemAllergen link : links) {
            if (link == null)
                continue;
            Allergen a = link.getAllergen();
            if (a == null)
                continue;

            AllergenStatus st = link.getStatus() == null ? AllergenStatus.CONTAINS : link.getStatus();
            String suffix = (st == AllergenStatus.MAY_CONTAIN) ? "__MAY" : "__CONTAINS";

            String code = a.getCode() == null ? "allergen" : a.getCode().toLowerCase(Locale.ITALY);
            String iconUrl = "/images/allergens/" + code + suffix + ".svg";

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

    /**
     * Converte stringa in nome immagine sicuro (non usata ai fini allergeni, utile
     * altrove).
     */
    private String toImageName(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim()
                .toLowerCase()
                .replace(" ", "_");
    }

    /** Applica selezioni allergeni al piatto (replace completo). */
    @Transactional
    protected void applyAllergens(MenuItem item, List<AllergenSelection> selections) {
        // pulizia esistente
        if (item.getId() != null) {
            menuItemAllergenRepository.deleteByMenuItem_Id(item.getId());
        }
        if (selections == null || selections.isEmpty())
            return;

        for (AllergenSelection sel : selections) {
            if (sel == null || sel.getCode() == null)
                continue;

            Allergen allergen = allergenRepository.findByCode(sel.getCode().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Allergen code not found: " + sel.getCode()));

            AllergenStatus status = AllergenStatus.CONTAINS;
            if (sel.getStatus() != null) {
                status = AllergenStatus.valueOf(sel.getStatus().toUpperCase());
            }
            if (status == AllergenStatus.FREE)
                continue; // FREE = non memorizzare

            // crea link
            MenuItemAllergen link = new MenuItemAllergen(item, allergen, status);
            menuItemAllergenRepository.save(link);
        }
    }

    @Transactional
    public void deleteMenuItemsByCategory(String category) {
        List<MenuItem> items = menuItemRepository.findByCategory_Name(category);
        for (MenuItem item : items) {
            deleteMenuItem(item.getId());
        }
    }
}