package com.corner.pub.dto.request;

public class ReservationRequest {
    @jakarta.validation.constraints.NotBlank(message = "Il nome è obbligatorio")
    private String name;

    @jakarta.validation.constraints.NotBlank(message = "Il cognome è obbligatorio")
    private String surname;

    @jakarta.validation.constraints.Pattern(regexp = "^\\d+$", message = "Il telefono deve contenere solo numeri")
    private String phone;
    private String date; // formato: "2025-07-02"
    private String time; // formato: "19:30"
    private int people;
    private String note;
    private Long eventId; // null per prenotazione normale
    private String tableNumber; // aggiunto

    private Boolean privacyAccepted;

    // Allergeni (facoltativo)
    private String allergensNote;
    private Boolean allergensConsent;

    // Getters & Setters

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getPeople() {
        return people;
    }

    public void setPeople(int people) {
        this.people = people;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Boolean getPrivacyAccepted() {
        return privacyAccepted;
    }

    public void setPrivacyAccepted(Boolean privacyAccepted) {
        this.privacyAccepted = privacyAccepted;
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
}
