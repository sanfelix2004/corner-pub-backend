package com.corner.pub.service;

import com.corner.pub.dto.request.BookingSettingsRequest;
import com.corner.pub.dto.response.BookingSettingsResponse;
import com.corner.pub.exception.ForbiddenException;
import com.corner.pub.model.BookingSettings;
import com.corner.pub.repository.BookingSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingSettingsService {

    public static final String TABLE_FULL_MESSAGE =
            "Siamo al completo: le prenotazioni tavolo sono momentaneamente chiuse.";
    public static final String EVENT_FULL_MESSAGE =
            "Siamo al completo: le iscrizioni agli eventi sono momentaneamente chiuse.";

    private final BookingSettingsRepository repository;

    public BookingSettingsService(BookingSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BookingSettingsResponse getSettings() {
        return new BookingSettingsResponse(getOrCreate());
    }

    @Transactional
    public BookingSettingsResponse update(BookingSettingsRequest request) {
        BookingSettings settings = getOrCreate();
        if (request.getTableBookingsBlocked() != null) {
            settings.setTableBookingsBlocked(request.getTableBookingsBlocked());
        }
        if (request.getEventBookingsBlocked() != null) {
            settings.setEventBookingsBlocked(request.getEventBookingsBlocked());
        }
        return new BookingSettingsResponse(repository.save(settings));
    }

    @Transactional(readOnly = true)
    public void assertTableBookingsOpen() {
        if (getOrCreate().isTableBookingsBlocked()) {
            throw new ForbiddenException(TABLE_FULL_MESSAGE);
        }
    }

    @Transactional(readOnly = true)
    public void assertEventBookingsOpen() {
        if (getOrCreate().isEventBookingsBlocked()) {
            throw new ForbiddenException(EVENT_FULL_MESSAGE);
        }
    }

    private BookingSettings getOrCreate() {
        return repository.findById(BookingSettings.SINGLETON_ID).orElseGet(() -> {
            BookingSettings created = new BookingSettings();
            created.setId(BookingSettings.SINGLETON_ID);
            created.setTableBookingsBlocked(false);
            created.setEventBookingsBlocked(false);
            return repository.save(created);
        });
    }
}
