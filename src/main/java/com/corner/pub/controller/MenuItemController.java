package com.corner.pub.controller;

import com.corner.pub.dto.request.MenuItemRequest;
import com.corner.pub.dto.response.MenuItemResponse;
import com.corner.pub.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuItemController {

    private final MenuItemService menuItemService;

    @Autowired
    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getAll() {
        return ResponseEntity.ok(menuItemService.getAllMenuItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponse> getById(@PathVariable Long id) {
        // se non trovato, MenuItemNotFoundException → 404 gestita globalmente
        MenuItemResponse dto = menuItemService.getMenuItemById(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<MenuItemResponse> create(@RequestBody MenuItemRequest request) {
        MenuItemResponse dto = menuItemService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // se non trovato, MenuItemNotFoundException → 404 gestita globalmente
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }
}
