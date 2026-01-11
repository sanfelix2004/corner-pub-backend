package com.corner.pub.dto.request;

import org.springframework.web.multipart.MultipartFile;

public class EventRegistrationRequest {
    @jakarta.validation.constraints.NotBlank(message = "Il nome è obbligatorio")
    private String name;

    @jakarta.validation.constraints.NotBlank(message = "Il cognome è obbligatorio")
    private String surname;

    @jakarta.validation.constraints.Pattern(regexp = "^\\d+$", message = "Il telefono deve contenere solo numeri")
    private String phone;
    private String note; // <-- aggiunto
    private Boolean privacyAccepted; // Aggiunto per GDPR

    // Allergeni
    private String allergensNote;
    private Boolean allergensConsent;

    private int partecipanti = 1; // default 1
    private MultipartFile poster;

    // GETTER & SETTER
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

    public MultipartFile getPoster() {
        return poster;
    }

    public void setPoster(MultipartFile poster) {
        this.poster = poster;
    }

    public Boolean getPrivacyAccepted() {
        return privacyAccepted;
    }

    public void setPrivacyAccepted(Boolean privacyAccepted) {
        this.privacyAccepted = privacyAccepted;
    }
}
