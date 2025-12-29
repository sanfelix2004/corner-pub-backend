package com.corner.pub.dto.request;

import java.util.List;

public class MenuItemRequest {
    private Long categoryId;
    private String categoryName; // opzionale, se vuoi creare nuove categorie al volo
    private String titolo;
    private String descrizione;
    private double prezzo;
    private java.util.List<AllergenSelection> allergens;

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
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

    public List<AllergenSelection> getAllergens() {
        return allergens;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public void setAllergens(List<AllergenSelection> allergens) {
        this.allergens = allergens;
    }

    // Compatibility method for frontend sending "categoria"
    public void setCategoria(String categoria) {
        this.categoryName = categoria;
    }
}
