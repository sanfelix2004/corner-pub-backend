package com.corner.pub.dto.response;

import java.util.List;


public class MenuItemResponse {
    private Long id;
    private String categoryName;
    private String titolo;
    private String descrizione;
    private double prezzo;
    private String imageUrl;
    private boolean visibile;
    private java.util.List<AllergenResponse> allergens;

    public void setId(Long id) {
        this.id = id;
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

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setVisibile(boolean visibile) {
        this.visibile = visibile;
    }

    public void setAllergens(List<AllergenResponse> allergens) {
        this.allergens = allergens;
    }

    public Long getId() {
        return id;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isVisibile() {
        return visibile;
    }

    public List<AllergenResponse> getAllergens() {
        return allergens;
    }
}
