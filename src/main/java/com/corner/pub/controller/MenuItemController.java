package com.corner.pub.controller;

import com.corner.pub.dto.request.MenuItemRequest;
import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/menu")
public class MenuItemController {

    private final MenuItemService menuItemService;

    @Autowired
    public MenuItemController(MenuItemService menuItemService) {
        System.out.println("🔥 RICHIESTA MENU RICEVUTA");
        this.menuItemService = menuItemService;
    }

    /**
     * Endpoint pubblico – restituisce solo i piatti visibili.
     * GET /api/menu
     */
    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getVisibleMenuItems() {
        System.out.println("🔥 RICHIESTA MENU RICEVUTA");
        return ResponseEntity.ok(menuItemService.getVisibleMenuItems());
    }

    /**
     * Endpoint da back office – restituisce tutto il menu, anche i piatti non visibili.
     * GET /api/menu/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<MenuItemResponse>> getAll() {
        System.out.println("🔥 RICHIESTA MENU RICEVUTA");
        return ResponseEntity.ok(menuItemService.getAllMenuItems());
    }

    /**
     * Recupera un piatto per ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponse> getById(@PathVariable Long id) {
        System.out.println("🔥 RICHIESTA MENU RICEVUTA");
        return ResponseEntity.ok(menuItemService.getMenuItemById(id));
    }
}
