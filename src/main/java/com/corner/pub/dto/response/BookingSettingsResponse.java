package com.corner.pub.dto.response;

import com.corner.pub.model.BookingSettings;

import java.time.Instant;

public class BookingSettingsResponse {

    private boolean tableBookingsBlocked;
    private boolean eventBookingsBlocked;
    private Instant updatedAt;

    public BookingSettingsResponse() {}

    public BookingSettingsResponse(BookingSettings settings) {
        this.tableBookingsBlocked = settings.isTableBookingsBlocked();
        this.eventBookingsBlocked = settings.isEventBookingsBlocked();
        this.updatedAt = settings.getUpdatedAt();
    }

    public boolean isTableBookingsBlocked() {
        return tableBookingsBlocked;
    }

    public void setTableBookingsBlocked(boolean tableBookingsBlocked) {
        this.tableBookingsBlocked = tableBookingsBlocked;
    }

    public boolean isEventBookingsBlocked() {
        return eventBookingsBlocked;
    }

    public void setEventBookingsBlocked(boolean eventBookingsBlocked) {
        this.eventBookingsBlocked = eventBookingsBlocked;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
