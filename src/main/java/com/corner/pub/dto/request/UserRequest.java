package com.corner.pub.dto.request;

public class UserRequest {
    @jakarta.validation.constraints.NotBlank(message = "Il nome è obbligatorio")
    private String name;

    @jakarta.validation.constraints.NotBlank(message = "Il cognome è obbligatorio")
    private String surname;

    @jakarta.validation.constraints.Pattern(regexp = "^\\d+$", message = "Il telefono deve contenere solo numeri")
    private String phone;

    // Getters & Setters
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
}
