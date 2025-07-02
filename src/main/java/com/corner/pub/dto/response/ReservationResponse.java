package com.corner.pub.dto.response;

public class ReservationResponse {
    private Long id;
    private String name;
    private String phone;
    private String date;
    private String time;
    private int people;
    private String note;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
}
