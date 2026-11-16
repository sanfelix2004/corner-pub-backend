package com.corner.pub.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Ordine di visualizzazione sul menù pubblico (0 = prima). */
    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(columnDefinition = "boolean default true")
    private Boolean active = true;

    /**
     * Se true, sul sito si mostra il prezzo multiplo
     * (1 = prezzo base, ogni unità in più = +extraUnitPrice).
     * Pensato per categorie smash, ma attivabile su qualsiasi categoria.
     */
    @Column(name = "quantity_pricing_enabled", nullable = false, columnDefinition = "boolean default false")
    private boolean quantityPricingEnabled = false;

    /** Supplemento per ogni unità oltre la prima (es. 2.00 €). */
    @Column(name = "extra_unit_price")
    private Double extraUnitPrice;

    /** Etichetta unità mostrata al cliente (es. "smash"). Se vuota: "pz". */
    @Column(name = "quantity_unit_label", length = 40)
    private String quantityUnitLabel;

    /** Quante unità massime mostrare nella tabella prezzi (default 3). */
    @Column(name = "max_quantity")
    private Integer maxQuantity;

    public Category(String name) {
        this.name = name;
    }
}
