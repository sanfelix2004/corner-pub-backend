package com.corner.pub.dto.response;

import com.corner.pub.model.enums.KitchenOrderStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class KitchenOrderResponse {
    private Long id;
    private Long tableSessionId;
    private KitchenOrderStatus status;
    private String generalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;
    private List<OrderItemResponse> items;
}
