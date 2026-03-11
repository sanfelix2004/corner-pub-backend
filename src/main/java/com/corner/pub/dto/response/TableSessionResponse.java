package com.corner.pub.dto.response;

import com.corner.pub.model.enums.TableStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TableSessionResponse {
    private Long id;
    private String tableNumber;
    private TableStatus status;
    private String generalNotes;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private String openedByUsername;
}
