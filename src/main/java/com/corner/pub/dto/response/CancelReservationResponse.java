package com.corner.pub.dto.response;

public class CancelReservationResponse {
    private boolean success;
    private String message;

    public CancelReservationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
