package com.corner.pub.dto.request;

public class BookingSettingsRequest {

    private Boolean tableBookingsBlocked;
    private Boolean eventBookingsBlocked;

    public Boolean getTableBookingsBlocked() {
        return tableBookingsBlocked;
    }

    public void setTableBookingsBlocked(Boolean tableBookingsBlocked) {
        this.tableBookingsBlocked = tableBookingsBlocked;
    }

    public Boolean getEventBookingsBlocked() {
        return eventBookingsBlocked;
    }

    public void setEventBookingsBlocked(Boolean eventBookingsBlocked) {
        this.eventBookingsBlocked = eventBookingsBlocked;
    }
}
