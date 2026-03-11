package com.corner.pub.dto.request;

import lombok.Data;

@Data
public class CreateTableSessionRequest {
    private String tableNumber;
    private String generalNotes;
}
