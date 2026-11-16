package com.corner.pub.dto.response;

import java.util.List;


public class MenuItemResponse {
    private Long id;
    private String categoryName;
    private Integer categorySortOrder;
    private String titolo;
    private String descrizione;
    private double prezzo;
    private String imageUrl;
    private boolean visibile;
    private java.util.List<AllergenResponse> allergens;

    /** Prezzo multiplo della categoria (es. smash: 1 base, ogni extra +X €). */
    private boolean quantityPricingEnabled;
    private Double extraUnitPrice;
    private String quantityUnitLabel;
    private Integer maxQuantity;

    public void setId(Long id) {
        this.id = id;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setCategorySortOrder(Integer categorySortOrder) {
        this.categorySortOrder = categorySortOrder;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public void setQuantityPricingEnabled(boolean quantityPricingEnabled) {
        this.quantityPricingEnabled = quantityPricingEnabled;
    }

    public void setExtraUnitPrice(Double extraUnitPrice) {
        this.extraUnitPrice = extraUnitPrice;
    }

    public void setQuantityUnitLabel(String quantityUnitLabel) {
        this.quantityUnitLabel = quantityUnitLabel;
    }

    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
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

    public Integer getCategorySortOrder() {
        return categorySortOrder;
    }

    public String getTitolo() {
        return titolo;
    }

    public boolean isQuantityPricingEnabled() {
        return quantityPricingEnabled;
    }

    public Double getExtraUnitPrice() {
        return extraUnitPrice;
    }

    public String getQuantityUnitLabel() {
        return quantityUnitLabel;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
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
