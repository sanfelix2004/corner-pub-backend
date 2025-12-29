package com.corner.pub.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationResponse {
    private Long id;
    private String name;
    private String surname;
    private String phone;
    private LocalDate date;
    private LocalTime time;
    private int people;
    private String note;
    private Long eventId; // Aggiungi questo campo
    private Boolean isEventRegistration; // Aggiungi questo campo
    private EventResponse event;
    private String tableNumber; // aggiunto

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public Boolean getEventRegistration() {
        return isEventRegistration;
    }

    public void setEventRegistration(Boolean eventRegistration) {
        isEventRegistration = eventRegistration;
    }

    public EventResponse getEvent() {
        return event;
    }

    public void setEvent(EventResponse event) {
        this.event = event;
    }

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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
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

    public Boolean getIsEventRegistration() {
        return isEventRegistration;
    }

    public void setIsEventRegistration(Boolean eventRegistration) {
        isEventRegistration = eventRegistration;
    }
}
