package com.corner.pub.model;

import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

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

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getTitolo() {
        return titolo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public boolean isVisibile() {
        return visibile;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Set<MenuItemAllergen> getAllergens() {
        return allergens;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    public void setVisibile(boolean visibile) {
        this.visibile = visibile;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setAllergens(Set<MenuItemAllergen> allergens) {
        this.allergens = allergens;
    }
}
