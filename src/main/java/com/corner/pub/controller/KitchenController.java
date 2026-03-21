package com.corner.pub.controller;

import com.corner.pub.model.KitchenOrder;
import com.corner.pub.model.OrderItem;
import com.corner.pub.model.enums.KitchenOrderStatus;
import com.corner.pub.model.enums.OrderItemStatus;
import com.corner.pub.service.KitchenOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cucina")
public class KitchenController {

    @Autowired
    private KitchenOrderService kitchenOrderService;

    @GetMapping("/orders/active")
    public ResponseEntity<List<KitchenOrder>> getActiveOrders() {
        return ResponseEntity.ok(kitchenOrderService.getActiveKitchenOrders());
    }

    @GetMapping("/orders/archived")
    public ResponseEntity<List<KitchenOrder>> getArchivedOrders() {
        return ResponseEntity.ok(kitchenOrderService.getArchivedKitchenOrders());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<KitchenOrder> getOrderDetails(@PathVariable Long id) {
        return ResponseEntity.ok(kitchenOrderService.getKitchenOrderById(id));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<KitchenOrder> updateOrderStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        KitchenOrderStatus status = KitchenOrderStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(kitchenOrderService.updateOrderStatus(id, status));
    }

    @PatchMapping("/orders/{id}/items/{itemId}/status")
    public ResponseEntity<OrderItem> updateItemStatus(@PathVariable Long id, @PathVariable Long itemId,
            @RequestBody Map<String, String> body) {
        OrderItemStatus status = OrderItemStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(kitchenOrderService.updateOrderItemStatus(id, itemId, status));
    }

    @PostMapping("/orders/{id}/archive")
    public ResponseEntity<Void> archiveOrder(@PathVariable Long id) {
        kitchenOrderService.archiveOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tables/{id}/archive")
    public ResponseEntity<Void> archiveTable(@PathVariable Long id) {
        kitchenOrderService.archiveTableSession(id);
        return ResponseEntity.noContent().build();
    }
}
