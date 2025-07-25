package com.corner.pub.dto.response;

import java.time.LocalDateTime;

public class EventRegistrationResponse {

    private String name;
    private String phone;
    private LocalDateTime createdAt;

    public EventRegistrationResponse() {
    }

    public EventRegistrationResponse(String name, String phone, LocalDateTime createdAt) {
        this.name = name;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    // Getters & Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
