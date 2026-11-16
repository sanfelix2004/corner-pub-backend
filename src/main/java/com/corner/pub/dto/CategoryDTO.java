package com.corner.pub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;
    private String name;
    private Integer sortOrder;
    private Boolean quantityPricingEnabled;
    private Double extraUnitPrice;
    private String quantityUnitLabel;
    private Integer maxQuantity;
}
