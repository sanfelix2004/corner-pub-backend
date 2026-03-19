package com.corner.pub.model;

import com.corner.pub.model.enums.KitchenOrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kitchen_orders")
@Data
@NoArgsConstructor
public class KitchenOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mapping directly by ID or Object depending on preference. Use JoinColumn for
    // Object.
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_session_id", nullable = false)
    private TableSession tableSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KitchenOrderStatus status = KitchenOrderStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String generalNotes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime sentAt;

    /**
     * Numero progressivo della comanda all'interno del tavolo (1, 2, 3...).
     * Impostato al momento dell'invio.
     */
    @Column
    private Integer commandaNumber;

    @OneToMany(mappedBy = "kitchenOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
