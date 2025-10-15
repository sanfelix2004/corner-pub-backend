package com.corner.pub.dto.request;

public class AllergenSelection {
    private String code;   // MILK
    private String status; // CONTAINS | MAY_CONTAIN | FREE

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}