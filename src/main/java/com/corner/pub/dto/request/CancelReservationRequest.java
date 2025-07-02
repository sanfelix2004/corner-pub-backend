package com.corner.pub.dto.request;

public class CancelReservationRequest {
    private String phone;
    private String date;

    // Getters & Setters
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
