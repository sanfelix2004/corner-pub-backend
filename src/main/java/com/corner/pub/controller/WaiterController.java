package com.corner.pub.controller;

import com.corner.pub.dto.request.AddOrderItemRequest;
import com.corner.pub.dto.request.CreateTableSessionRequest;
import com.corner.pub.dto.request.UpdateOrderItemRequest;
import com.corner.pub.model.KitchenOrder;
import com.corner.pub.model.OrderItem;
import com.corner.pub.model.TableSession;
import com.corner.pub.model.enums.TableStatus;
import com.corner.pub.service.CategoryService;
import com.corner.pub.service.KitchenOrderService;
import com.corner.pub.service.MenuItemService;
import com.corner.pub.service.TableSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cameriere")
public class WaiterController {

    @Autowired
    private TableSessionService tableSessionService;
    @Autowired
    private KitchenOrderService kitchenOrderService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private MenuItemService menuItemService;

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // --- TABLES ---

    @PostMapping("/tables")
    public ResponseEntity<TableSession> openTable(@RequestBody CreateTableSessionRequest req) {
        return ResponseEntity.ok(tableSessionService.createTableSession(req, getCurrentUsername()));
    }

    @GetMapping("/tables/open")
    public ResponseEntity<List<TableSession>> getOpenTables() {
        return ResponseEntity.ok(tableSessionService.getOpenTables());
    }

    @GetMapping("/tables/{id}")
    public ResponseEntity<TableSession> getTableDetails(@PathVariable Long id) {
        return ResponseEntity.ok(tableSessionService.getTableSessionById(id));
    }

    @PatchMapping("/tables/{id}")
    public ResponseEntity<TableSession> updateTable(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        String notes = updates.get("generalNotes");
        String statusStr = updates.get("status");
        TableStatus status = statusStr != null ? TableStatus.valueOf(statusStr) : null;
        return ResponseEntity.ok(tableSessionService.updateTableSession(id, null, notes, status));
    }

    @PostMapping("/tables/{id}/close")
    public ResponseEntity<TableSession> closeTable(@PathVariable Long id) {
        return ResponseEntity.ok(tableSessionService.closeTableSession(id));
    }

    // --- MENU (Forward to standard endpoints if convenient, or implement specific
    // simple views here) ---

    @GetMapping("/menu/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories()); // Will need sorting but reusing existing
    }

    @GetMapping("/menu/items")
    public ResponseEntity<?> getMenuItems(@RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String query) {
        // Simple forward. Note: In a real system we'd filter by active/available.
        return ResponseEntity.ok(menuItemService.getAllMenuItems());
    }

    // --- ORDERS ---

    @GetMapping("/orders/active")
    public ResponseEntity<List<KitchenOrder>> getActiveOrders() {
        return ResponseEntity.ok(kitchenOrderService.getActiveKitchenOrders());
    }

    @GetMapping("/orders/archived")
    public ResponseEntity<List<KitchenOrder>> getArchivedOrders() {
        return ResponseEntity.ok(kitchenOrderService.getArchivedKitchenOrders());
    }

    @PostMapping("/tables/{tableId}/orders")
    public ResponseEntity<KitchenOrder> createDraftOrder(@PathVariable Long tableId) {
        return ResponseEntity.ok(kitchenOrderService.getOrCreateDraftOrder(tableId));
    }

    @GetMapping("/tables/{tableId}/orders/current")
    public ResponseEntity<KitchenOrder> getCurrentOrder(@PathVariable Long tableId) {
        KitchenOrder order = kitchenOrderService.getCurrentOrder(tableId);
        if (order == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(order);
    }

    @PostMapping("/orders/{id}/items")
    public ResponseEntity<OrderItem> addOrderItem(@PathVariable Long id, @RequestBody AddOrderItemRequest req) {
        return ResponseEntity.ok(kitchenOrderService.addOrderItem(id, req));
    }

    @PatchMapping("/orders/{id}/items/{itemId}")
    public ResponseEntity<OrderItem> updateOrderItem(@PathVariable Long id, @PathVariable Long itemId,
            @RequestBody UpdateOrderItemRequest req) {
        return ResponseEntity.ok(kitchenOrderService.updateOrderItem(id, itemId, req));
    }

    @DeleteMapping("/orders/{id}/items/{itemId}")
    public ResponseEntity<Void> removeOrderItem(@PathVariable Long id, @PathVariable Long itemId) {
        kitchenOrderService.removeOrderItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/orders/{id}/notes")
    public ResponseEntity<KitchenOrder> updateNotes(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(kitchenOrderService.updateOrderNotes(id, body.get("generalNotes")));
    }

    @PostMapping("/orders/{id}/send")
    public ResponseEntity<KitchenOrder> sendOrder(@PathVariable Long id) {
        return ResponseEntity.ok(kitchenOrderService.sendOrder(id));
    }
}
