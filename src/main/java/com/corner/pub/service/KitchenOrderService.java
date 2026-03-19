package com.corner.pub.service;

import com.corner.pub.dto.KitchenEventPayload;
import com.corner.pub.dto.request.AddOrderItemRequest;
import com.corner.pub.dto.request.UpdateOrderItemRequest;
import com.corner.pub.model.KitchenOrder;
import com.corner.pub.model.MenuItem;
import com.corner.pub.model.OrderItem;
import com.corner.pub.model.TableSession;
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

    /**
     * Invia un payload strutturato via WebSocket a tutti i client connessi.
     * Sostituisce la vecchia stringa generica "UPDATE" con un oggetto tipizzato
     * che permette ai client di discriminare il tipo di evento.
     *
     * eventType può essere:
     * NEW_ORDER — nuova comanda inviata dal cameriere (triggera audio + toast in
     * cucina)
     * STATUS_CHANGED — stato della comanda aggiornato dalla cucina
     * ARCHIVED — comanda archiviata
     * ITEM_CHANGED — item aggiunto/modificato/rimosso su comanda già inviata
     */
    private void notifyKitchen(KitchenOrder order, String eventType) {
        TableSession ts = order.getTableSession();
        KitchenEventPayload payload = new KitchenEventPayload(
                eventType,
                order.getId(),
                ts != null ? ts.getTableNumber() : null,
                ts != null ? ts.getCopeRti() : null,
                order.getStatus().name());
        messagingTemplate.convertAndSend("/topic/kitchen/orders", payload);
    }

    @Transactional
    public KitchenOrder getOrCreateDraftOrder(Long tableSessionId) {
        TableSession table = tableSessionRepository.findById(tableSessionId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        // Cerca SOLO una comanda in stato DRAFT per questo tavolo.
        // Se il cameriere ha già inviato la comanda #1 (SENT/IN_PREPARATION/DONE),
        // questo metodo crea una nuova DRAFT (comanda #2) invece di restituire quella
        // in corso.
        Optional<KitchenOrder> existingDraft = kitchenOrderRepository
                .findByTableSessionIdAndStatus(tableSessionId, KitchenOrderStatus.DRAFT);

        if (existingDraft.isPresent()) {
            return existingDraft.get();
        }

        KitchenOrder newOrder = new KitchenOrder();
        newOrder.setTableSession(table);
        newOrder.setStatus(KitchenOrderStatus.DRAFT);
        return kitchenOrderRepository.save(newOrder);
    }

    public KitchenOrder getCurrentOrder(Long tableSessionId) {
        // Cerca solo il DRAFT corrente: la comanda che il cameriere sta componendo.
        // Le comande già inviate (SENT, IN_PREPARATION, DONE) rimangono visibili
        // nello storico ma non sono la "comanda corrente".
        return kitchenOrderRepository.findByTableSessionIdAndStatus(tableSessionId, KitchenOrderStatus.DRAFT)
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
            notifyKitchen(order, "ITEM_CHANGED");
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
            notifyKitchen(item.getKitchenOrder(), "ITEM_CHANGED");
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
            notifyKitchen(order, "ITEM_CHANGED");
        }
    }

    @Transactional
    public KitchenOrder updateOrderNotes(Long orderId, String notes) {
        KitchenOrder order = kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setGeneralNotes(notes);
        KitchenOrder saved = kitchenOrderRepository.save(order);
        if (saved.getStatus() != KitchenOrderStatus.DRAFT) {
            notifyKitchen(saved, "ITEM_CHANGED");
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

            // Calcola il numero progressivo della comanda per questo tavolo.
            // Conta quante comande (non DRAFT) esistono già per lo stesso tavolo.
            Long tableSessionId = order.getTableSession().getId();
            long previouslySent = kitchenOrderRepository.findByTableSessionId(tableSessionId)
                    .stream()
                    .filter(o -> o.getStatus() != KitchenOrderStatus.DRAFT && !o.getId().equals(orderId))
                    .count();
            order.setCommandaNumber((int) previouslySent + 1);

            // update table status
            TableSession table = order.getTableSession();
            table.setStatus(TableStatus.SENT_TO_KITCHEN);
            tableSessionRepository.save(table);
        }

        KitchenOrder saved = kitchenOrderRepository.save(order);
        // NEW_ORDER è l'evento che triggera suono + toast in cucina
        notifyKitchen(saved, "NEW_ORDER");
        return saved;
    }

    // --- Kitchen Flow ---

    public List<KitchenOrder> getActiveKitchenOrders() {
        return kitchenOrderRepository.findByStatusNotInOrderByCreatedAtAsc(
                java.util.Arrays.asList(KitchenOrderStatus.ARCHIVED, KitchenOrderStatus.DRAFT));
    }

    public List<KitchenOrder> getArchivedKitchenOrders() {
        LocalDateTime twelveHoursAgo = LocalDateTime.now().minusHours(12);
        return kitchenOrderRepository.findByStatusAndUpdatedAtAfterOrderByUpdatedAtDesc(
                KitchenOrderStatus.ARCHIVED, twelveHoursAgo);
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

        // Ottimizzazione stato di produzione:
        // Quando un ticket tavolo intero avanza in produzione in cucina,
        // cascatamento automatico del relativo stato ai singoli piatti contenuti.
        OrderItemStatus targetItemStatus = null;
        if (status == KitchenOrderStatus.IN_PREPARATION) {
            targetItemStatus = OrderItemStatus.IN_PREPARATION;
        } else if (status == KitchenOrderStatus.DONE) {
            targetItemStatus = OrderItemStatus.DONE;
        }

        if (targetItemStatus != null) {
            for (OrderItem item : order.getItems()) {
                item.setStatus(targetItemStatus);
            }
        }

        KitchenOrder saved = kitchenOrderRepository.save(order);
        notifyKitchen(saved, "STATUS_CHANGED");
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
        notifyKitchen(item.getKitchenOrder(), "STATUS_CHANGED");
        return saved;
    }

    @Transactional
    public void archiveOrder(Long orderId) {
        KitchenOrder order = kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(KitchenOrderStatus.ARCHIVED);
        KitchenOrder saved = kitchenOrderRepository.save(order);

        // Chiude il tavolo quando tutte le comande INVIATE (non-DRAFT) sono archiviate.
        // I DRAFT vuoti pendenti vengono ignorati nel check e poi eliminati.
        // Senza questo fix la bozza vuota della comanda successiva bloccava la
        // chiusura.
        TableSession ts = order.getTableSession();
        if (ts != null && ts.getStatus() != com.corner.pub.model.enums.TableStatus.CLOSED) {
            List<KitchenOrder> allOrders = kitchenOrderRepository.findByTableSessionId(ts.getId());

            // Considera solo le comande realmente inviate (non DRAFT)
            List<KitchenOrder> sentOrders = allOrders.stream()
                    .filter(o -> o.getStatus() != KitchenOrderStatus.DRAFT)
                    .toList();

            boolean allArchived = !sentOrders.isEmpty() &&
                    sentOrders.stream().allMatch(o -> o.getStatus() == KitchenOrderStatus.ARCHIVED);

            if (allArchived) {
                // Elimina eventuali bozze vuote rimaste aperte prima di chiudere il tavolo
                allOrders.stream()
                        .filter(o -> o.getStatus() == KitchenOrderStatus.DRAFT)
                        .forEach(kitchenOrderRepository::delete);

                ts.setStatus(com.corner.pub.model.enums.TableStatus.CLOSED);
                ts.setClosedAt(LocalDateTime.now());
                tableSessionRepository.save(ts);
            }
        }

        notifyKitchen(saved, "ARCHIVED");
    }

    /**
     * Restituisce tutte le comande (incluse ARCHIVED) per un dato tavolo.
     * Usato dal cameriere per vedere lo storico completo del tavolo.
     */
    public List<KitchenOrder> getOrdersByTableSession(Long tableSessionId) {
        return kitchenOrderRepository.findByTableSessionId(tableSessionId);
    }
}
