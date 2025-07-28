package com.corner.pub.dto.request;

public class ReservationRequest {
    private String name;
    private String phone;
    private String date;   // formato: "2025-07-02"
    private String time;   // formato: "19:30"
    private int people;
    private String note;
    private Long eventId; // null per prenotazione normale

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getPeople() { return people; }
    public void setPeople(int people) { this.people = people; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
