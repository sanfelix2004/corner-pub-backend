package com.corner.pub.dto.response;

import com.corner.pub.model.User;

public class UserResponse {
    private Long id;
    private String name;
    private String surname;
    private String phone;
    private String privacyPolicyVersion;

    public UserResponse(UserResponse user) {
        this.id = user.getId();
    }

    public UserResponse() {
    }

    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.surname = user.getSurname();
        this.phone = user.getPhone();
        this.privacyPolicyVersion = user.getPrivacyPolicyVersion();
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPrivacyPolicyVersion() {
        return privacyPolicyVersion;
    }

    public void setPrivacyPolicyVersion(String privacyPolicyVersion) {
        this.privacyPolicyVersion = privacyPolicyVersion;
    }
}
