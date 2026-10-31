package com.corner.pub.repository;

import com.corner.pub.model.BookingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSettingsRepository extends JpaRepository<BookingSettings, Long> {
}
