package com.corner.pub.service;

import com.corner.pub.dto.request.EventRequest;
import com.corner.pub.dto.request.SpecialeRequest;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.dto.response.SpecialeResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.exception.resourcenotfound.ResourceNotFoundException;
import com.corner.pub.model.Speciale;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.repository.SpecialeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpecialeService {

    private static final Set<String> TEMI = Set.of("default", "oktoberfest", "natale", "estate", "notte");

    private final SpecialeRepository specialeRepository;
    private final StorageService storageService;
    private final EventService eventService;
    private final EventRepository eventRepository;

    public List<SpecialeResponse> getAll() {
        return specialeRepository.findAllByOrderByDataInizioDesc().stream()
                .map(SpecialeResponse::new)
                .toList();
    }

    public List<SpecialeResponse> getActive() {
        return specialeRepository.findActiveValid(LocalDate.now()).stream()
                .map(SpecialeResponse::new)
                .toList();
    }

    public SpecialeResponse getById(Long id) {
        return new SpecialeResponse(require(id));
    }

    @Transactional
    public SpecialeResponse create(SpecialeRequest request, MultipartFile image) {
        validateDates(request);
        Speciale s = new Speciale();
        apply(s, request);
        Speciale saved = specialeRepository.save(s);
        if (image != null && !image.isEmpty()) {
            storeImage(saved, image, false);
            saved = specialeRepository.save(saved);
        }
        if (Boolean.TRUE.equals(request.getCreaEventoSubordinato())) {
            saved = attachLinkedEvent(saved, request);
        }
        return new SpecialeResponse(saved);
    }

    @Transactional
    public SpecialeResponse update(Long id, SpecialeRequest request, MultipartFile image) {
        validateDates(request);
        Speciale s = require(id);
        apply(s, request);
        if (image != null && !image.isEmpty()) {
            storeImage(s, image, true);
        }
        return new SpecialeResponse(specialeRepository.save(s));
    }

    @Transactional
    public SpecialeResponse setAttivo(Long id, boolean attivo) {
        Speciale s = require(id);
        s.setAttivo(attivo);
        return new SpecialeResponse(specialeRepository.save(s));
    }

    @Transactional
    public void delete(Long id) {
        Speciale s = require(id);
        if (s.getEventoId() != null) {
            eventRepository.findById(s.getEventoId()).ifPresent(event -> {
                event.setSpecialeId(null);
                eventRepository.save(event);
            });
        }
        storageService.deleteOwned(s.getImmagineUrl(), "speciali", String.valueOf(id));
        storageService.delete(s.getImmaginePublicId());
        specialeRepository.delete(s);
    }

    private Speciale require(Long id) {
        return specialeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Speciale non trovato con ID: " + id));
    }

    private void apply(Speciale s, SpecialeRequest request) {
        s.setTitolo(trim(request.getTitolo()));
        s.setKicker(blankToNull(request.getKicker()));
        s.setDescrizione(blankToNull(request.getDescrizione()));
        s.setDataInizio(request.getDataInizio());
        s.setDataFine(request.getDataFine());
        if (s.getId() == null || blankToNull(request.getTema()) != null) {
            s.setTema(normalizeTema(request.getTema()));
        }
        s.setCtaTesto(blankToNull(request.getCtaTesto()));
        s.setCtaCategoria(blankToNull(request.getCtaCategoria()));
        s.setCtaUrl(blankToNull(request.getCtaUrl()));
        s.setGiorniSettimana(RecurrenceSupport.serialize(request.getGiorniSettimana()));
        if (request.getMostraPrenotati() != null) {
            s.setMostraPrenotati(Boolean.TRUE.equals(request.getMostraPrenotati()));
        }
        if (request.getAttivo() != null) {
            s.setAttivo(request.getAttivo());
        }
    }

    private Speciale attachLinkedEvent(Speciale speciale, SpecialeRequest request) {
        String mode = request.getEventoPeriodoMode() == null
                ? "STESSO"
                : request.getEventoPeriodoMode().trim().toUpperCase(Locale.ROOT);
        boolean dedicated = "RICORRENTE".equals(mode) || "DEDICATO".equals(mode);
        String giorni = dedicated
                ? RecurrenceSupport.serialize(request.getEventoGiorniSettimana())
                : RecurrenceSupport.serialize(speciale.getGiorniSettimana());
        boolean forever = speciale.getDataFine() == null;
        boolean ricorrente = giorni != null && !giorni.isBlank();

        if (dedicated && !ricorrente) {
            throw new CornerPubException("Per il periodo ricorrente dedicato scegli almeno un giorno (es. venerdì)");
        }
        if (!dedicated && forever && !ricorrente) {
            throw new CornerPubException(
                    "Uno speciale per sempre senza giorni non può copiare il periodo sull'evento. Scegli i giorni dello speciale oppure un ricorrente dedicato.");
        }

        LocalTime ora = request.getEventoOra() != null ? request.getEventoOra() : LocalTime.of(20, 0);
        EventRequest eventRequest = new EventRequest();
        String titolo = blankToNull(request.getEventoTitolo());
        eventRequest.setTitolo(titolo != null ? titolo : speciale.getTitolo());
        eventRequest.setDescrizione(speciale.getDescrizione());
        eventRequest.setData(RecurrenceSupport.firstOccurrence(speciale.getDataInizio(), ora, giorni));
        eventRequest.setPostiTotali(request.getEventoPostiTotali());
        eventRequest.setRicorrente(ricorrente);
        eventRequest.setGiorniSettimana(giorni);
        eventRequest.setDataFine(speciale.getDataFine());
        eventRequest.setSpecialeId(speciale.getId());

        EventResponse created = eventService.createEvent(eventRequest, null);
        eventService.copyPosterFromUrl(created.getId(), speciale.getImmagineUrl());

        speciale.setEventoId(created.getId());
        if (request.getMostraPrenotati() == null || Boolean.TRUE.equals(request.getMostraPrenotati())) {
            speciale.setMostraPrenotati(true);
        }
        return specialeRepository.save(speciale);
    }

    private void storeImage(Speciale s, MultipartFile image, boolean replace) {
        try {
            String relative = replace
                    ? storageService.replace("speciali", String.valueOf(s.getId()), s.getImmagineUrl(), image)
                    : storageService.store("speciali", String.valueOf(s.getId()), image);
            s.setImmagineUrl(storageService.publicUrl(relative));
            s.setImmaginePublicId(relative);
        } catch (Exception e) {
            log.error("Errore upload immagine speciale: {}", e.getMessage());
            throw new CornerPubException("Impossibile salvare l'immagine dello speciale");
        }
    }

    private void validateDates(SpecialeRequest request) {
        if (request.getDataInizio() != null && request.getDataFine() != null
                && request.getDataFine().isBefore(request.getDataInizio())) {
            throw new CornerPubException("La data fine deve essere successiva alla data inizio");
        }
    }

    private String normalizeTema(String tema) {
        String value = tema == null ? "default" : tema.trim().toLowerCase(Locale.ROOT);
        return TEMI.contains(value) ? value : "default";
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
