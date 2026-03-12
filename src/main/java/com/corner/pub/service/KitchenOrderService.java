package com.corner.pub.service;

import com.corner.pub.dto.request.AddOrderItemRequest;
import com.corner.pub.dto.request.UpdateOrderItemRequest;
import com.corner.pub.model.KitchenOrder;
import com.corner.pub.model.MenuItem;
import com.corner.pub.model.OrderItem;
import com.corner.pub.model.TableSession;
import com.corner.pub.model.enums.TableStatus;
import com.corner.pub.model.enums.KitchenOrderStatus;
import com.corner.pub.model.enums.OrderItemStatus;
import com.corner.pub.model.enums.TableStatus;
import com.corner.pub.repository.KitchenOrderRepository;
import com.corner.pub.repository.MenuItemRepository;
import com.corner.pub.repository.OrderItemRepository;
import com.corner.pub.repository.TableSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class KitchenOrderService {

    @Autowired
    private KitchenOrderRepository kitchenOrderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private TableSessionRepository tableSessionRepository;
    @Autowired
    private MenuItemRepository menuItemRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private void notifyKitchen() {
        messagingTemplate.convertAndSend("/topic/kitchen/orders", "UPDATE");
    }

    @Transactional
    public KitchenOrder getOrCreateDraftOrder(Long tableSessionId) {
        TableSession table = tableSessionRepository.findById(tableSessionId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        Optional<KitchenOrder> activeOrder = kitchenOrderRepository.findByTableSessionIdAndStatusNot(
                tableSessionId, KitchenOrderStatus.ARCHIVED);

        if (activeOrder.isPresent()) {
            return activeOrder.get();
        }

        KitchenOrder newOrder = new KitchenOrder();
        newOrder.setTableSession(table);
        newOrder.setStatus(KitchenOrderStatus.DRAFT);
        return kitchenOrderRepository.save(newOrder);
    }

    public KitchenOrder getCurrentOrder(Long tableSessionId) {
        return kitchenOrderRepository.findByTableSessionIdAndStatusNot(tableSessionId, KitchenOrderStatus.ARCHIVED)
                .orElse(null);
    }

    @Transactional
    public OrderItem addOrderItem(Long orderId, AddOrderItemRequest req) {
        KitchenOrder order = kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        MenuItem menuItem = menuItemRepository.findById(req.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        // Match exact menuItemId and note
        Optional<OrderItem> existingItem = order.getItems().stream()
                .filter(i -> i.getMenuItemId().equals(menuItem.getId()) &&
                        ((i.getNote() == null && req.getNote() == null) ||
                                (i.getNote() != null && i.getNote().equals(req.getNote()))))
                .findFirst();

        OrderItem item;
        if (existingItem.isPresent()) {
            item = existingItem.get();
            item.setQuantity(item.getQuantity() + (req.getQuantity() != null ? req.getQuantity() : 1));
        } else {
            item = new OrderItem();
            item.setKitchenOrder(order);
            item.setMenuItemId(menuItem.getId());
            item.setMenuItemNameSnapshot(menuItem.getTitolo());
            item.setMenuItemDescriptionSnapshot(menuItem.getDescrizione());
            item.setQuantity(req.getQuantity() != null ? req.getQuantity() : 1);
            item.setNote(req.getNote());
            order.getItems().add(item);
        }

        OrderItem saved = orderItemRepository.save(item);
        if (order.getStatus() != KitchenOrderStatus.DRAFT) {
            notifyKitchen();
        }
        return saved;
    }

    @Transactional
    public OrderItem updateOrderItem(Long orderId, Long itemId, UpdateOrderItemRequest req) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        if (!item.getKitchenOrder().getId().equals(orderId)) {
            throw new RuntimeException("Item does not belong to order");
        }

        if (req.getQuantity() != null)
            item.setQuantity(req.getQuantity());
        if (req.getNote() != null)
            item.setNote(req.getNote());

        OrderItem saved = orderItemRepository.save(item);
        if (item.getKitchenOrder().getStatus() != KitchenOrderStatus.DRAFT) {
            notifyKitchen();
        }
        return saved;
    }

    @Transactional
    public void removeOrderItem(Long orderId, Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        KitchenOrder order = item.getKitchenOrder();
        if (!order.getId().equals(orderId)) {
            throw new RuntimeException("Item does not belong to order");
        }
        orderItemRepository.delete(item);
        if (order.getStatus() != KitchenOrderStatus.DRAFT) {
            notifyKitchen();
        }
    }

    @Transactional
    public KitchenOrder updateOrderNotes(Long orderId, String notes) {
        KitchenOrder order = kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setGeneralNotes(notes);
        KitchenOrder saved = kitchenOrderRepository.save(order);
        if (saved.getStatus() != KitchenOrderStatus.DRAFT) {
            notifyKitchen();
        }
        return saved;
    }

    @Transactional
    public KitchenOrder sendOrder(Long orderId) {
        KitchenOrder order = kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getItems().isEmpty()) {
            throw new RuntimeException("Cannot send an empty order");
        }

        if (order.getStatus() == KitchenOrderStatus.DRAFT) {
            order.setStatus(KitchenOrderStatus.SENT);
            order.setSentAt(LocalDateTime.now());

            // update table status
            TableSession table = order.getTableSession();
            table.setStatus(TableStatus.SENT_TO_KITCHEN);
            tableSessionRepository.save(table);
        }

        KitchenOrder saved = kitchenOrderRepository.save(order);
        notifyKitchen();
        return saved;
    }

    // --- Kitchen Flow ---

    public List<KitchenOrder> getActiveKitchenOrders() {
        return kitchenOrderRepository.findByStatusNot(KitchenOrderStatus.ARCHIVED);
    }

    public List<KitchenOrder> getArchivedKitchenOrders() {
        LocalDateTime twelveHoursAgo = LocalDateTime.now().minusHours(12);
        return kitchenOrderRepository.findAll().stream()
                .filter(o -> o.getStatus() == KitchenOrderStatus.ARCHIVED &&
                        o.getUpdatedAt() != null &&
                        o.getUpdatedAt().isAfter(twelveHoursAgo))
                .toList();
    }

    /**
     * Per il cameriere: non mostrare più le comande "finite" o archiviate tra le attive.
     * (Nel flusso cucina, DONE resta ancora "attiva" finché non viene archiviata esplicitamente.)
     */
    @Transactional(readOnly = true)
    public List<KitchenOrder> getActiveKitchenOrdersForWaiter() {
        return kitchenOrderRepository.findByStatusNotIn(List.of(KitchenOrderStatus.DONE, KitchenOrderStatus.ARCHIVED));
    }

    /**
     * Per il cameriere: storico ultime 12h include sia ARCHIVED che DONE.
     */
    @Transactional(readOnly = true)
    public List<KitchenOrder> getArchivedKitchenOrdersForWaiter() {
        LocalDateTime twelveHoursAgo = LocalDateTime.now().minusHours(12);
        return kitchenOrderRepository.findByStatusIn(List.of(KitchenOrderStatus.DONE, KitchenOrderStatus.ARCHIVED))
                .stream()
                .filter(o -> o.getUpdatedAt() != null && o.getUpdatedAt().isAfter(twelveHoursAgo))
                .toList();
    }

    public KitchenOrder getKitchenOrderById(Long orderId) {
        return kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional
    public KitchenOrder updateOrderStatus(Long orderId, KitchenOrderStatus status) {
        KitchenOrder order = kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        KitchenOrder saved = kitchenOrderRepository.save(order);
        notifyKitchen();
        return saved;
    }

    @Transactional
    public OrderItem updateOrderItemStatus(Long orderId, Long itemId, OrderItemStatus status) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        if (!item.getKitchenOrder().getId().equals(orderId)) {
            throw new RuntimeException("Item does not belong to order");
        }
        item.setStatus(status);
        OrderItem saved = orderItemRepository.save(item);
        notifyKitchen();
        return saved;
    }

    @Transactional
    public void archiveOrder(Long orderId) {
        KitchenOrder order = kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(KitchenOrderStatus.ARCHIVED);
        kitchenOrderRepository.save(order);

        // Se non esistono altre comande non archiviate per quel tavolo,
        // chiudi automaticamente la sessione tavolo così non appare più tra i "tavoli aperti".
        TableSession table = order.getTableSession();
        if (table != null) {
            Long tableId = table.getId();
            boolean hasOtherActiveOrders = kitchenOrderRepository
                    .findByTableSessionIdAndStatusNot(tableId, KitchenOrderStatus.ARCHIVED)
                    .isPresent();
            if (!hasOtherActiveOrders) {
                table.setStatus(TableStatus.CLOSED);
                table.setClosedAt(LocalDateTime.now());
                tableSessionRepository.save(table);
            }
        }
        notifyKitchen();
    }
}
