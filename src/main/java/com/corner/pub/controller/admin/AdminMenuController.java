package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.MenuItemRequest;
import com.corner.pub.dto.response.AllergenResponse;
import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.model.AllergenStatus;
import com.corner.pub.repository.AllergenRepository;
import com.corner.pub.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/menu")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuItemService menuItemService;
    private final AllergenRepository allergenRepository;

    // ✅ Ottieni tutto il menu (anche quelli nascosti)
    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getAllMenuItems() {
        return ResponseEntity.ok(menuItemService.getAllMenuItems());
    }

    // ✅ Aggiungi un nuovo piatto (con immagine opzionale)
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<MenuItemResponse> addMenuItem(
            @RequestPart("data") MenuItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        MenuItemResponse response = menuItemService.addMenuItem(request, image);
        return ResponseEntity.ok(response);
    }

    // ✅ Modifica un piatto esistente (con immagine opzionale)
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable Long id,
            @RequestPart("data") MenuItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        MenuItemResponse response = menuItemService.updateMenuItem(id, request, image);
        return ResponseEntity.ok(response);
    }

    // ✅ Elimina un piatto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Attiva/disattiva visibilità
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<MenuItemResponse> toggleVisibility(@PathVariable Long id) {
        MenuItemResponse updated = menuItemService.toggleVisibility(id);
        return ResponseEntity.ok(updated);
    }

    // ADD: elenco allergeni per il picker
    // AdminMenuController.java
    @GetMapping("/allergens")
    public List<AllergenResponse> listAllergens() {
        return allergenRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(a -> {
                    AllergenResponse r = new AllergenResponse();
                    r.setCode(a.getCode());
                    r.setLabel(a.getLabel());
                    // se hai iconBase in entity:
                    r.setIconUrl("/img/" + a.getIconBase() + ".png");
                    // lo status per l’anagrafica è solo un default lato UI:
                    r.setStatus("FREE");
                    return r;
                })
                .toList();
    }

}
