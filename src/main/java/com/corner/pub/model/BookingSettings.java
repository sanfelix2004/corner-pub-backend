package com.corner.pub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Impostazioni singleton (id = 1) per chiudere le prenotazioni pubbliche
 * quando il locale è pieno. Il back office può comunque inserire a mano.
 */
@Entity
@Table(name = "booking_settings")
public class BookingSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "table_bookings_blocked", nullable = false)
    private boolean tableBookingsBlocked = false;

    @Column(name = "event_bookings_blocked", nullable = false)
    private boolean eventBookingsBlocked = false;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
        if (id == null) {
            id = SINGLETON_ID;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
