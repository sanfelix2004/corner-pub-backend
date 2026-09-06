package com.corner.pub.service;

import com.corner.pub.dto.request.EventRequest;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.model.Event;
import com.corner.pub.repository.EventRegistrationRepository;
import com.corner.pub.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final StorageService storageService;

    @Transactional
    public EventResponse createEvent(EventRequest request, MultipartFile poster) {
        Event event = new Event();
        event.setTitolo(request.getTitolo());
        event.setDescrizione(request.getDescrizione());
        event.setData(request.getData());
        event.setPostiTotali(request.getPostiTotali());

        Event saved = eventRepository.save(event);

        if (poster != null && !poster.isEmpty()) {
            try {
                String relative = storageService.store("eventi", String.valueOf(saved.getId()), poster);
                saved.setPosterUrl(storageService.publicUrl(relative));
                saved.setPosterPublicId(relative);
                saved = eventRepository.save(saved);
            } catch (Exception e) {
                log.error("Errore durante l'upload della locandina: {}", e.getMessage());
                throw new CornerPubException("Impossibile salvare la locandina dell'evento");
            }
        }

        return new EventResponse(saved, 0);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request, MultipartFile poster) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        event.setTitolo(request.getTitolo());
        event.setDescrizione(request.getDescrizione());
        event.setData(request.getData());
        event.setPostiTotali(request.getPostiTotali());

        if (poster != null && !poster.isEmpty()) {
            try {
                String relative = storageService.replace(
                        "eventi", String.valueOf(event.getId()), event.getPosterUrl(), poster);
                event.setPosterUrl(storageService.publicUrl(relative));
                event.setPosterPublicId(relative);
            } catch (Exception e) {
                log.error("Errore upload nuova locandina: {}", e.getMessage());
                throw new CornerPubException("Impossibile aggiornare la locandina dell'evento");
            }
        }

        Event updated = eventRepository.save(event);
        long totalePartecipanti = registrationRepository.countByEventId(eventId);
        return new EventResponse(updated, totalePartecipanti);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        storageService.deleteOwned(event.getPosterUrl(), "eventi", String.valueOf(eventId));
        storageService.delete(event.getPosterPublicId());

        registrationRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }
}
