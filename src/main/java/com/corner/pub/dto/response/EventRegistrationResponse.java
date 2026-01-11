package com.corner.pub.dto.response;

import com.corner.pub.model.EventRegistration;

import java.time.LocalDateTime;

public class EventRegistrationResponse {

    private String name;
    private String surname;
    private Long id;
    private LocalDateTime createdAt;
    private EventResponse event;
    private UserResponse user;
    private int partecipanti;
    private String phone;
    private String note;
    private String tableNumber; // 🔹 nuovo campo
    private String allergensNote; // 🔹 nuovo campo
    private String privacyPolicyVersion; // 🔹 nuovo campo

    public EventRegistrationResponse() {
    }

    public EventRegistrationResponse(EventRegistration reg) {
        this.id = reg.getId();
        this.createdAt = reg.getCreatedAt();

        if (reg.getUser() != null) {
            this.user = new UserResponse(reg.getUser());
            this.name = reg.getUser().getName();
            this.surname = reg.getUser().getSurname();
            this.phone = reg.getUser().getPhone();
            this.partecipanti = reg.getPartecipanti();
        }

        if (reg.getEvent() != null) {
            long totaleIscritti = reg.getEvent().getRegistrations() != null
                    ? reg.getEvent().getRegistrations().stream()
                            .mapToLong(EventRegistration::getPartecipanti)
                            .sum()
                    : reg.getPartecipanti();

            this.event = new EventResponse(reg.getEvent(), totaleIscritti);
        }

        this.note = reg.getNote();
        this.tableNumber = reg.getTableNumber(); // 🔹 assegno il tavolo
        this.allergensNote = reg.getAllergensNote();
        this.privacyPolicyVersion = reg.getPrivacyPolicyVersion();
    }

    public EventRegistrationResponse(Long id, LocalDateTime createdAt, EventResponse eventResponse,
            UserResponse userResponse, int partecipanti) {
        this.id = id;
        this.createdAt = createdAt;
        this.event = eventResponse;
        this.user = userResponse;
        this.partecipanti = partecipanti;
    }

    // Getter & Setter
    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public EventResponse getEvent() {
        return event;
    }

    public void setEvent(EventResponse event) {
        this.event = event;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public int getPartecipanti() {
        return partecipanti;
    }

    public void setPartecipanti(int partecipanti) {
        this.partecipanti = partecipanti;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getAllergensNote() {
        return allergensNote;
    }

    public void setAllergensNote(String allergensNote) {
        this.allergensNote = allergensNote;
    }

    public String getPrivacyPolicyVersion() {
        return privacyPolicyVersion;
    }

    public void setPrivacyPolicyVersion(String privacyPolicyVersion) {
        this.privacyPolicyVersion = privacyPolicyVersion;
    }
}
