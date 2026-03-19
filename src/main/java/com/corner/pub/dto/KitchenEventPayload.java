package com.corner.pub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload strutturato inviato via WebSocket ai client (cucina e cameriere)
 * al posto della generica stringa "UPDATE".
 * Permette ai client di discriminare il tipo di evento e reagire diversamente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KitchenEventPayload {

    /**
     * Tipo di evento:
     * - NEW_ORDER: nuova comanda inviata dal cameriere
     * - STATUS_CHANGED: stato di una comanda aggiornato dalla cucina
     * - ARCHIVED: comanda archiviata
     * - ITEM_CHANGED: item di una comanda aggiornato
     */
    private String eventType;

    private Long orderId;
    private String tableNumber;
    private Integer copeRti;
    private String status;
}
