package com.corner.pub.dto.response;

import com.corner.pub.model.enums.OrderItemStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderItemResponse {
    private Long id;
    private Long menuItemId;
    private String menuItemNameSnapshot;
    private int quantity;
    private String note;
    private OrderItemStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
