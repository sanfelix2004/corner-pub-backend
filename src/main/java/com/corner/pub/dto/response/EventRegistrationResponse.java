package com.corner.pub.dto.response;

import com.corner.pub.model.EventRegistration;

import java.time.LocalDateTime;

public class EventRegistrationResponse {

    private String name;
    private Long id;
    private LocalDateTime createdAt;
    private EventResponse event;
    private UserResponse user;
    private Integer partecipanti;
    private String phone;

    public EventRegistrationResponse() {}

    public EventRegistrationResponse(EventRegistration reg) {
        this.id = reg.getId();
        this.createdAt = reg.getCreatedAt();
        if (reg.getUser() != null) {
            this.user = new UserResponse(reg.getUser());
            this.name = reg.getUser().getName();
            this.phone = reg.getUser().getPhone();
        }
        if (reg.getEvent() != null) {
            this.event = new EventResponse(reg.getEvent());
        }
    }


    public EventRegistrationResponse(Long id, LocalDateTime createdAt, EventResponse event, UserResponse user, Integer partecipanti) {
        this.id = id;
        this.createdAt = createdAt;
        this.event = event;
        this.user = user;
        this.partecipanti = partecipanti;
    }

    public EventRegistrationResponse(String name, String phone, LocalDateTime createdAt) {
        this.createdAt = createdAt;
        this.phone = phone;
        this.name = name;

    }

    public EventRegistrationResponse(Long id, LocalDateTime createdAt, EventResponse eventResponse, UserResponse userResponse) {
        this.id = id;
        this.createdAt = createdAt;
        this.event = eventResponse;
        this.user = userResponse;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public EventResponse getEvent() { return event; }
    public void setEvent(EventResponse event) { this.event = event; }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }

    public Integer getPartecipanti() { return partecipanti; }
    public void setPartecipanti(Integer partecipanti) { this.partecipanti = partecipanti; }

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
}
