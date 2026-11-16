package com.corner.pub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private String titolo;
    private String descrizione;
    private double prezzo;

    @Column(nullable = false)
    private boolean visibile = true; // default true

    @Column(name = "imageurl")
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(columnDefinition = "boolean default true")
    private Boolean available = true;

    /**
     * Prezzo multiplo per questo piatto (es. 1/2/3 smash).
     * Si attiva piatto per piatto dal back office.
     */
    @Column(name = "quantity_pricing_enabled", nullable = false, columnDefinition = "boolean default false")
    private boolean quantityPricingEnabled = false;

    @Column(name = "extra_unit_price")
    private Double extraUnitPrice;

    @Column(name = "quantity_unit_label", length = 40)
    private String quantityUnitLabel;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    // Compatibilità per il codice che usa getCategoria()
    public String getCategoria() {
        return category != null ? category.getName() : null;
    }

    // Rimuoviamo setCategoria perché non possiamo settare una Category da stringa
    // qui senza repository.
    // Il service deve occuparsene.

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

    public boolean isQuantityPricingEnabled() {
        return quantityPricingEnabled;
    }

    public void setQuantityPricingEnabled(boolean quantityPricingEnabled) {
        this.quantityPricingEnabled = quantityPricingEnabled;
    }

    public Double getExtraUnitPrice() {
        return extraUnitPrice;
    }

    public void setExtraUnitPrice(Double extraUnitPrice) {
        this.extraUnitPrice = extraUnitPrice;
    }

    public String getQuantityUnitLabel() {
        return quantityUnitLabel;
    }

    public void setQuantityUnitLabel(String quantityUnitLabel) {
        this.quantityUnitLabel = quantityUnitLabel;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }
}
