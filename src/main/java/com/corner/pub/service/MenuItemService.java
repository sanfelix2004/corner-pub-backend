package com.corner.pub.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.corner.pub.dto.request.MenuItemRequest;
import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.exception.conflictexception.MenuItemDuplicateException;
import com.corner.pub.exception.resourcenotfound.MenuItemNotFoundException;
import com.corner.pub.model.MenuItem;
import com.corner.pub.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final Cloudinary cloudinary;

    @Autowired
    public MenuItemService(MenuItemRepository menuItemRepository, Cloudinary cloudinary) {
        this.menuItemRepository = menuItemRepository;
        this.cloudinary = cloudinary;
    }

    /**
     * Restituisce l’intero menu.
     */
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Aggiunge un nuovo piatto e carica l’immagine su Cloudinary.
     */
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

        // ⚠️ Prima salvo per ottenere l'ID
        MenuItem saved = menuItemRepository.save(item);

        // 📤 Upload immagine con nome = id
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                Map uploadResult = cloudinary.uploader().upload(image.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "prodotti/",
                                "public_id", String.valueOf(saved.getId()), // o item.getId()
                                "overwrite", true,
                                "invalidate", true,  // 🔁 forza aggiornamento cache
                                "resource_type", "image"
                        ));

                imageUrl = (String) uploadResult.get("secure_url");
            } catch (Exception e) {
                throw new RuntimeException("Errore durante il caricamento immagine su Cloudinary", e);
            }
        }

        return mapToResponse(saved, imageUrl);
    }


    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));
        return mapToResponse(item);
    }

    /**
     * Modifica un piatto esistente, ricarica immagine se presente.
     */
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request, MultipartFile image) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));

        item.setCategoria(request.getCategoria().trim());
        item.setTitolo(request.getTitolo().trim());
        item.setDescrizione(request.getDescrizione());
        item.setPrezzo(request.getPrezzo());

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                Map uploadResult = cloudinary.uploader().upload(image.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "prodotti/",
                                "public_id", String.valueOf(item.getId()),
                                "overwrite", true,
                                "resource_type", "image"
                        ));
                imageUrl = (String) uploadResult.get("secure_url");
            } catch (Exception e) {
                throw new RuntimeException("Errore nel caricamento immagine Cloudinary", e);
            }
        }

        MenuItem updated = menuItemRepository.save(item);
        return mapToResponse(updated, imageUrl);
    }

    /**
     * Elimina un piatto per ID.
     */
    public void deleteMenuItem(Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new MenuItemNotFoundException(id);
        }
        menuItemRepository.deleteById(id);
    }

    /**
     * Inverte la visibilità (true/false).
     */
    public MenuItemResponse toggleVisibility(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));
        item.setVisibile(!item.isVisibile());
        MenuItem updated = menuItemRepository.save(item);
        return mapToResponse(updated);
    }

    /**
     * Mappa MenuItem → Response con immagine ricostruita
     */
    private MenuItemResponse mapToResponse(MenuItem item) {
        String generatedUrl = cloudinary.url()
                .secure(true)
                .version(System.currentTimeMillis() / 1000) // 🔁 versione dinamica
                .generate("prodotti/" + item.getId());
        return mapToResponse(item, generatedUrl);
    }




    /**
     * Mappa MenuItem → Response con immagine personalizzata
     */
    private MenuItemResponse mapToResponse(MenuItem item, String imageUrl) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(item.getId());
        response.setCategoria(item.getCategoria());
        response.setTitolo(item.getTitolo());
        response.setDescrizione(item.getDescrizione());
        response.setPrezzo(item.getPrezzo());
        response.setVisibile(item.isVisibile());
        response.setImageUrl(imageUrl);
        return response;
    }

    /**
     * Utility: genera nome sicuro per Cloudinary.
     */
    private String toImageName(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim()
                .toLowerCase()
                .replace(" ", "_");
    }

    public List<MenuItemResponse> getVisibleMenuItems() {
        return menuItemRepository.findAll().stream()
                .filter(MenuItem::isVisibile)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


}
