package com.corner.pub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String categoria; // ← nuovo campo

    private String titolo;
    private String descrizione;
    private double prezzo;

    @Column(nullable = false)
    private boolean visibile = true; // default true

    @Column(name = "imageurl")
    private String imageUrl;

    // ... esistente ...
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<MenuItemAllergen> allergens = new java.util.HashSet<>();

    // Getters & Setters

    public java.util.Set<MenuItemAllergen> getAllergens() {
        return allergens;
    }

    public void setAllergens(java.util.Set<MenuItemAllergen> allergens) {
        this.allergens = allergens;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

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

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    public boolean isVisibile() {
        return visibile;
    }

    public void setVisibile(boolean visibile) {
        this.visibile = visibile;
    }
}
