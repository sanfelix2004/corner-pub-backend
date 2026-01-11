package com.corner.pub.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_registration", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "event_id" }))
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private int partecipanti;

    @Column
    private String note;

    @Column(name = "allergens_note")
    private String allergensNote;

    @Column(name = "allergens_consent")
    private Boolean allergensConsent;

    @Column
    private String tableNumber;

    @Column(name = "privacy_policy_version")
    private String privacyPolicyVersion;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAllergensNote() {
        return allergensNote;
    }

    public void setAllergensNote(String allergensNote) {
        this.allergensNote = allergensNote;
    }

    public Boolean getAllergensConsent() {
        return allergensConsent;
    }

    public void setAllergensConsent(Boolean allergensConsent) {
        this.allergensConsent = allergensConsent;
    }

    public int getPartecipanti() {
        return partecipanti;
    }

    public void setPartecipanti(int partecipanti) {
        this.partecipanti = partecipanti;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getPrivacyPolicyVersion() {
        return privacyPolicyVersion;
    }

    public void setPrivacyPolicyVersion(String privacyPolicyVersion) {
        this.privacyPolicyVersion = privacyPolicyVersion;
    }
}
