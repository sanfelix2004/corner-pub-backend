// src/main/java/com/corner/pub/model/InEvidenza.java
package com.corner.pub.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "in_evidenza")
public class InEvidenza {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private MenuItem prodotto;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters & setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public MenuItem getProdotto() {
        return prodotto;
    }

    public void setProdotto(MenuItem prodotto) {
        this.prodotto = prodotto;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}