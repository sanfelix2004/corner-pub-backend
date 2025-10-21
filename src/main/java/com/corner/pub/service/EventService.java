package com.corner.pub.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final Cloudinary cloudinary;

    private String toPublicId(String title) {
        if (title == null || title.isBlank()) return "event";
        String base = java.text.Normalizer.normalize(title.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^A-Za-z0-9 _-]", "")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_");
        return base.isBlank() ? "event" : base;
    }

    // -----------------------------
    //           CREATE
    // -----------------------------
    @Transactional
    public EventResponse createEvent(EventRequest request, MultipartFile poster) {
        Event event = new Event();
        event.setTitolo(request.getTitolo());
        event.setDescrizione(request.getDescrizione());
        event.setData(request.getData());
        event.setPostiTotali(request.getPostiTotali());

        // Upload locandina su Cloudinary
        if (poster != null && !poster.isEmpty()) {
            try {
                String publicId = toPublicId(request.getTitolo());
                var uploadResult = cloudinary.uploader().upload(
                        poster.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "event/",
                                "public_id", publicId,
                                "overwrite", true,
                                "unique_filename", false,
                                "invalidate", true,
                                "resource_type", "image"
                        )
                );
                event.setPosterUrl(uploadResult.get("secure_url").toString());
                event.setPosterPublicId(uploadResult.get("public_id").toString());
            } catch (IOException e) {
                log.error("Errore durante l'upload della locandina: {}", e.getMessage());
            }
        }

        Event saved = eventRepository.save(event);
        return new EventResponse(saved, 0);
    }

    // -----------------------------
    //           UPDATE
    // -----------------------------
    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request, MultipartFile poster) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        event.setTitolo(request.getTitolo());
        event.setDescrizione(request.getDescrizione());
        event.setData(request.getData());
        event.setPostiTotali(request.getPostiTotali());
        String newPublicId = toPublicId(request.getTitolo());
        String oldPublicId = event.getPosterPublicId();

        if (poster != null && !poster.isEmpty()) {
            try {
                var uploadResult = cloudinary.uploader().upload(
                        poster.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "event/",
                                "public_id", newPublicId,
                                "overwrite", true,
                                "unique_filename", false,
                                "invalidate", true,
                                "resource_type", "image"
                        )
                );
                // se il publicId è cambiato, prova a cancellare l'asset precedente
                if (oldPublicId != null && !oldPublicId.equals("event/" + newPublicId) && !oldPublicId.equals(newPublicId)) {
                    try { cloudinary.uploader().destroy(oldPublicId, ObjectUtils.asMap("invalidate", true)); } catch (IOException ex) { log.warn("Impossibile cancellare vecchia locandina {}: {}", oldPublicId, ex.getMessage()); }
                }
                event.setPosterUrl(uploadResult.get("secure_url").toString());
                event.setPosterPublicId(uploadResult.get("public_id").toString());
            } catch (IOException e) {
                log.error("Errore upload nuova locandina: {}", e.getMessage());
            }
        } else {
            // Nessun nuovo file: se è cambiato solo il titolo e abbiamo un poster, rinomina su Cloudinary per mantenere coerenza
            if (oldPublicId != null) {
                String target = oldPublicId.contains("/") ? (oldPublicId.substring(0, oldPublicId.lastIndexOf('/')+1) + newPublicId) : ("event/" + newPublicId);
                if (!oldPublicId.equals(target)) {
                    try {
                        var ren = cloudinary.uploader().rename(oldPublicId, target, ObjectUtils.asMap("overwrite", true, "invalidate", true));
                        event.setPosterPublicId(ren.get("public_id").toString());
                        // Nota: secure_url potrebbe cambiare se hai CDN/caching
                        Object url = ren.get("secure_url");
                        if (url != null) event.setPosterUrl(url.toString());
                    } catch (IOException ex) {
                        log.warn("Rinomina poster fallita da {} a {}: {}", oldPublicId, target, ex.getMessage());
                    }
                }
            }
        }

        Event updated = eventRepository.save(event);
        long totalePartecipanti = registrationRepository.countByEventId(eventId);
        return new EventResponse(updated, totalePartecipanti);
    }

    // -----------------------------
    //           DELETE
    // -----------------------------
    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        // Cancella locandina da Cloudinary
        if (event.getPosterPublicId() != null) {
            try {
                cloudinary.uploader().destroy(event.getPosterPublicId(), ObjectUtils.asMap("invalidate", true));
            } catch (IOException e) {
                log.warn("Errore cancellazione locandina Cloudinary: {}", e.getMessage());
            }
        }

        // Cancella registrazioni e evento
        registrationRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }
}