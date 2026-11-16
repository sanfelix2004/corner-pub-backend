package com.corner.pub.dto.response;

public class CategoryPublicResponse {
    private Long id;
    private String name;
    private Integer sortOrder;
    private boolean quantityPricingEnabled;
    private Double extraUnitPrice;
    private String quantityUnitLabel;
    private Integer maxQuantity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public boolean isQuantityPricingEnabled() { return quantityPricingEnabled; }
    public void setQuantityPricingEnabled(boolean quantityPricingEnabled) {
        this.quantityPricingEnabled = quantityPricingEnabled;
    }

    public Double getExtraUnitPrice() { return extraUnitPrice; }
    public void setExtraUnitPrice(Double extraUnitPrice) { this.extraUnitPrice = extraUnitPrice; }

    public String getQuantityUnitLabel() { return quantityUnitLabel; }
    public void setQuantityUnitLabel(String quantityUnitLabel) {
        this.quantityUnitLabel = quantityUnitLabel;
    }

    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }
}
