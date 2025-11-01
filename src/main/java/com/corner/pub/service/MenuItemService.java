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
import com.corner.pub.model.MenuItem;
import com.corner.pub.model.MenuItemAllergen;
import com.corner.pub.repository.AllergenRepository;
import com.corner.pub.repository.MenuItemAllergenRepository;
import com.corner.pub.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final Cloudinary cloudinary;

    private static final Logger log = LoggerFactory.getLogger(MenuItemService.class);

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Autowired
    public MenuItemService(MenuItemRepository menuItemRepository,
                           MenuItemAllergenRepository menuItemAllergenRepository,
                           AllergenRepository allergenRepository,
                           Cloudinary cloudinary) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemAllergenRepository = menuItemAllergenRepository;
        this.allergenRepository = allergenRepository;
        this.cloudinary = cloudinary;
    }

    /** Restituisce l’intero menu (visibili + nascosti). */
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Aggiunge un nuovo piatto e carica l’immagine su Cloudinary. */
    @Transactional
    public MenuItemResponse addMenuItem(MenuItemRequest request, MultipartFile image) {
        String categoria = request.getCategoria().trim();
        String titolo = request.getTitolo().trim();

        boolean exists = menuItemRepository.findAll().stream()
                .anyMatch(i -> i.getCategoria().equalsIgnoreCase(categoria)
                        && i.getTitolo().equalsIgnoreCase(titolo));
        if (exists) throw new MenuItemDuplicateException(titolo);

        MenuItem item = new MenuItem();
        item.setCategoria(categoria);
        item.setTitolo(titolo);
        item.setDescrizione(request.getDescrizione());
        item.setPrezzo(request.getPrezzo());
        item.setVisibile(true);

        // salvo per ottenere l'ID
        MenuItem saved = menuItemRepository.save(item);

        // Allergeni (relazioni)
        applyAllergens(saved, request.getAllergens());

        // upload immagine se presente
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
                // Non interrompere il flow: il piatto rimane salvato anche senza immagine
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

        item.setCategoria(request.getCategoria().trim());
        item.setTitolo(request.getTitolo().trim());
        item.setDescrizione(request.getDescrizione());
        item.setPrezzo(request.getPrezzo());

        // Allergeni (replace completo in base alla richiesta)
        applyAllergens(item, request.getAllergens());

        // upload nuova immagine se presente
        if (image != null && !image.isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(image.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "prodotti/",
                                "public_id", String.valueOf(item.getId()),
                                "overwrite", true,
                                "invalidate", true,
                                "resource_type", "image"
                        ));
                String imageUrl = (String) uploadResult.get("secure_url");
                item.setImageUrl(imageUrl);
            } catch (Exception e) {
                log.warn("Cloudinary upload failed on update for menuItem id={}: {}", item.getId(), e.getMessage());
                // Non interrompere l'update: mantieni le altre modifiche
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

        // 2) elimina asset Cloudinary (tenta sia /prodotti/<id> che <id> legacy)
        try {
            String publicIdPreferred = "prodotti/" + id;   // forma corretta
            Map<String, Object> opts = ObjectUtils.asMap(
                    "resource_type", "image",
                    "type", "upload",
                    "invalidate", true
            );

            // principale
            cloudinary.uploader().destroy(publicIdPreferred, opts);

            // eventuali derivati (thumbnail, trasformazioni, versioni)
            cloudinary.api().deleteResourcesByPrefix(publicIdPreferred, ObjectUtils.asMap(
                    "resource_type", "image",
                    "type", "upload"
            ));

            // legacy: se in passato hai salvato senza folder o con public_id "prodotti/<id>" in root
            cloudinary.uploader().destroy(String.valueOf(id), opts);

        } catch (Exception e) {
            log.warn("Cloudinary delete failed for menuItem id={}: {}", id, e.getMessage());
            // non bloccare la cancellazione del record
        }

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
        response.setCategoria(item.getCategoria());
        response.setTitolo(item.getTitolo());
        response.setDescrizione(item.getDescrizione());
        response.setPrezzo(item.getPrezzo());
        response.setVisibile(item.isVisibile());
        response.setImageUrl(item.getImageUrl());

        // Carica i link da repository (no lazy su item) e costruisci DTO in modo null-safe
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

    /** Converte stringa in nome immagine sicuro (non usata ai fini allergeni, utile altrove). */
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
        if (selections == null || selections.isEmpty()) return;

        for (AllergenSelection sel : selections) {
            if (sel == null || sel.getCode() == null) continue;

            Allergen allergen = allergenRepository.findByCode(sel.getCode().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Allergen code not found: " + sel.getCode()));

            AllergenStatus status = AllergenStatus.CONTAINS;
            if (sel.getStatus() != null) {
                status = AllergenStatus.valueOf(sel.getStatus().toUpperCase());
            }
            if (status == AllergenStatus.FREE) continue; // FREE = non memorizzare

            // crea link
            MenuItemAllergen link = new MenuItemAllergen(item, allergen, status);
            menuItemAllergenRepository.save(link);
        }
    }
}