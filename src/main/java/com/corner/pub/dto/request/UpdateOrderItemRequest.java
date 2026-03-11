package com.corner.pub.dto.request;

import lombok.Data;

@Data
public class UpdateOrderItemRequest {
    private Integer quantity;
    private String note;
}
