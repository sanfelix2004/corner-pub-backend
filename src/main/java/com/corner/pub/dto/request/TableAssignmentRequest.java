package com.corner.pub.dto.request;

import lombok.Getter;
import lombok.Setter;

public class TableAssignmentRequest {
    private String tableNumber;

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }
}
