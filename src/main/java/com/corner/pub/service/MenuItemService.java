package com.corner.pub.service;

import com.corner.pub.dto.request.MenuItemRequest;
import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.exception.conflictexception.MenuItemDuplicateException;
import com.corner.pub.exception.resourcenotfound.MenuItemNotFoundException;
import com.corner.pub.model.MenuItem;
import com.corner.pub.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;

    @Autowired
    public MenuItemService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    /**
     * Restituisce tutti gli item del menù.
     */
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuovo piatto. Lancia MenuItemDuplicateException se titolo già presente.
     */
    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        String categoria = request.getCategoria().trim();
        String titolo = request.getTitolo().trim();

        // duplicato su titolo e categoria?
        boolean exists = menuItemRepository.findAll().stream()
                .anyMatch(i ->
                        i.getCategoria().equalsIgnoreCase(categoria)
                                && i.getTitolo().equalsIgnoreCase(titolo)
                );
        if (exists) {
            throw new MenuItemDuplicateException(titolo);
        }

        MenuItem item = new MenuItem();
        item.setCategoria(categoria);                  // ← set category
        item.setTitolo(titolo);
        item.setDescrizione(request.getDescrizione());
        item.setPrezzo(request.getPrezzo());

        MenuItem saved = menuItemRepository.save(item);
        return mapToResponse(saved);
    }

    /**
     * Recupera un item per ID. Lancia MenuItemNotFoundException se non trovato.
     */
    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException(id));
        return mapToResponse(item);
    }

    /**
     * Elimina un item per ID. Lancia MenuItemNotFoundException se non trovato.
     */
    public void deleteMenuItem(Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new MenuItemNotFoundException(id);
        }
        menuItemRepository.deleteById(id);
    }

    private MenuItemResponse mapToResponse(MenuItem item) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(item.getId());
        response.setCategoria(item.getCategoria());    // ← include category
        response.setTitolo(item.getTitolo());
        response.setDescrizione(item.getDescrizione());
        response.setPrezzo(item.getPrezzo());
        return response;
    }
}
