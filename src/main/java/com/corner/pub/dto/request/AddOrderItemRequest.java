package com.corner.pub.dto.request;

import lombok.Data;

@Data
public class AddOrderItemRequest {
    private Long menuItemId;
    private Integer quantity;
    private String note;
}
