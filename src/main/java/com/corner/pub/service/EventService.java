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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final int OCCURRENCE_HORIZON_WEEKS = 16;

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final StorageService storageService;

    @Transactional
    public EventResponse createEvent(EventRequest request, MultipartFile poster) {
        boolean speciale = Boolean.TRUE.equals(request.getSpeciale());
        boolean ricorrente = Boolean.TRUE.equals(request.getRicorrente());

        if (ricorrente && request.getGiornoSettimana() == null) {
            throw new CornerPubException("Per un evento ricorrente seleziona il giorno della settimana");
        }

        LocalDateTime firstDate = resolveFirstOccurrence(
                request.getData(),
                ricorrente,
                request.getGiornoSettimana()
        );

        Event event = new Event();
        event.setTitolo(request.getTitolo());
        event.setDescrizione(request.getDescrizione());
        event.setData(firstDate);
        event.setPostiTotali(request.getPostiTotali());
        event.setSpeciale(speciale);
        event.setRicorrente(ricorrente);
        event.setGiornoSettimana(ricorrente ? request.getGiornoSettimana() : null);
        event.setDataFine(ricorrente ? request.getDataFine() : null);

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

        if (speciale && Boolean.TRUE.equals(request.getCreaEventoNormale())) {
            createSubordinateNormals(saved, request);
        } else if (ricorrente && !speciale) {
            // Evento normale ricorrente: materializza le prossime date
            materializeOccurrences(saved, saved.getGiornoSettimana(), saved.getDataFine(), OCCURRENCE_HORIZON_WEEKS);
        }

        return toResponse(saved);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request, MultipartFile poster) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        boolean ricorrente = Boolean.TRUE.equals(request.getRicorrente());
        if (ricorrente && request.getGiornoSettimana() == null) {
            throw new CornerPubException("Per un evento ricorrente seleziona il giorno della settimana");
        }

        LocalDateTime firstDate = resolveFirstOccurrence(
                request.getData(),
                ricorrente,
                request.getGiornoSettimana()
        );

        event.setTitolo(request.getTitolo());
        event.setDescrizione(request.getDescrizione());
        event.setData(firstDate);
        event.setPostiTotali(request.getPostiTotali());
        if (request.getSpeciale() != null) {
            event.setSpeciale(request.getSpeciale());
        }
        event.setRicorrente(ricorrente);
        event.setGiornoSettimana(ricorrente ? request.getGiornoSettimana() : null);
        event.setDataFine(ricorrente ? request.getDataFine() : null);

        if (poster != null && !poster.isEmpty()) {
            replaceStoredPoster(event, poster);
        }

        Event updated = eventRepository.save(event);

        // Se è uno speciale con figli, allinea titolo/descrizione/posti e rimpinisce l'orizzonte
        if (updated.isSpeciale()) {
            syncChildrenMetadata(updated);
            if (Boolean.TRUE.equals(request.getCreaEventoNormale())
                    || eventRepository.countByParentEventId(updated.getId()) > 0) {
                ensureChildrenHorizon(updated, request);
            }
        } else if (updated.isRicorrente() && updated.getParentEvent() == null) {
            materializeOccurrences(updated, updated.getGiornoSettimana(), updated.getDataFine(), OCCURRENCE_HORIZON_WEEKS);
        }

        return toResponse(updated);
    }

    @Transactional
    public EventResponse replacePoster(Long eventId, MultipartFile poster) {
        if (poster == null || poster.isEmpty()) {
            throw new CornerPubException("Seleziona una locandina da caricare");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));
        replaceStoredPoster(event, poster);
        Event updated = eventRepository.save(event);
        return toResponse(updated);
    }

    @Transactional
    public EventResponse deletePoster(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));
        storageService.deleteOwned(event.getPosterUrl(), "eventi", String.valueOf(eventId));
        storageService.delete(event.getPosterPublicId());
        event.setPosterUrl(null);
        event.setPosterPublicId(null);
        Event updated = eventRepository.save(event);
        return toResponse(updated);
    }

    private void replaceStoredPoster(Event event, MultipartFile poster) {
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

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        // Elimina prima i figli subordinati
        List<Event> children = eventRepository.findByParentEventIdOrderByDataAsc(eventId);
        for (Event child : children) {
            storageService.deleteOwned(child.getPosterUrl(), "eventi", String.valueOf(child.getId()));
            storageService.delete(child.getPosterPublicId());
            registrationRepository.deleteByEventId(child.getId());
            eventRepository.delete(child);
        }

        storageService.deleteOwned(event.getPosterUrl(), "eventi", String.valueOf(eventId));
        storageService.delete(event.getPosterPublicId());

        registrationRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    /**
     * Assicura che le serie ricorrenti abbiano date prenotabili nei prossimi mesi.
     * Chiamato dalle API pubbliche di listing.
     */
    @Transactional
    public void ensureRecurringHorizons() {
        List<Event> specials = eventRepository.findBySpecialeTrueAndRicorrenteTrue();
        for (Event special : specials) {
            if (eventRepository.countByParentEventId(special.getId()) > 0) {
                ensureChildrenHorizon(special, null);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listAllAdmin() {
        return eventRepository.findAllByOrderByDataAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Eventi prenotabili dal pubblico: date future normali (incluse subordinate)
     * e speciali senza figli. Gli speciali "template" con figli restano nascosti
     * perché si prenota la data subordinata.
     */
    @Transactional
    public List<EventResponse> listBookablePublic(LocalDate filterDate) {
        ensureRecurringHorizons();

        List<Event> candidates;
        if (filterDate != null) {
            LocalDateTime startOfDay = filterDate.atStartOfDay();
            LocalDateTime endOfDay = filterDate.plusDays(1).atStartOfDay();
            candidates = eventRepository.findByDataBetween(startOfDay, endOfDay);
        } else {
            candidates = eventRepository.findByDataAfterOrderByDataAsc(LocalDateTime.now());
        }

        Set<Long> parentsWithChildren = new HashSet<>();
        for (Event e : candidates) {
            if (e.getParentEvent() != null) {
                parentsWithChildren.add(e.getParentEvent().getId());
            }
        }
        // Anche speciali futuri che hanno già figli oltre l'orizzonte filtrato
        for (Event e : eventRepository.findBySpecialeTrueAndRicorrenteTrue()) {
            if (eventRepository.countByParentEventId(e.getId()) > 0) {
                parentsWithChildren.add(e.getId());
            }
        }

        List<EventResponse> out = new ArrayList<>();
        for (Event event : candidates) {
            if (event.isSpeciale() && parentsWithChildren.contains(event.getId())) {
                continue; // si prenota tramite le date subordinate
            }
            Event detailed = eventRepository.findDetailedById(event.getId()).orElse(event);
            out.add(toResponse(detailed));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long eventId) {
        Event event = eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));
        return toResponse(event);
    }

    private void createSubordinateNormals(Event special, EventRequest request) {
        String mode = request.getPeriodoNormale() == null
                ? "STESSO"
                : request.getPeriodoNormale().trim().toUpperCase(Locale.ROOT);

        if ("RICORRENTE".equals(mode)) {
            Integer day = request.getGiornoSettimanaNormale() != null
                    ? request.getGiornoSettimanaNormale()
                    : request.getGiornoSettimana();
            if (day == null) {
                throw new CornerPubException("Seleziona il giorno per l'evento normale ricorrente");
            }
            LocalDate end = request.getDataFineNormale() != null
                    ? request.getDataFineNormale()
                    : request.getDataFine();
            materializeChildOccurrences(special, day, end, OCCURRENCE_HORIZON_WEEKS);
        } else {
            // STESSO periodo della specialità
            if (special.isRicorrente()) {
                materializeChildOccurrences(
                        special,
                        special.getGiornoSettimana(),
                        special.getDataFine(),
                        OCCURRENCE_HORIZON_WEEKS
                );
            } else {
                createChildIfAbsent(special, special.getData());
            }
        }
    }

    private void ensureChildrenHorizon(Event special, EventRequest request) {
        Integer day = special.getGiornoSettimana();
        LocalDate end = special.getDataFine();
        if (request != null && "RICORRENTE".equalsIgnoreCase(
                request.getPeriodoNormale() != null ? request.getPeriodoNormale() : "")) {
            if (request.getGiornoSettimanaNormale() != null) {
                day = request.getGiornoSettimanaNormale();
            }
            if (request.getDataFineNormale() != null) {
                end = request.getDataFineNormale();
            }
        }
        if (day == null) {
            // non ricorrente: assicurati almeno la data dello speciale
            createChildIfAbsent(special, special.getData());
            return;
        }
        materializeChildOccurrences(special, day, end, OCCURRENCE_HORIZON_WEEKS);
    }

    private void materializeOccurrences(Event seriesRoot, Integer isoDay, LocalDate end, int weeks) {
        // Per serie senza parent: crea "cloni" normali subordinati a se stesso? No —
        // se un normale è ricorrente senza essere speciale, materializziamo come figli di sé.
        // Meglio: solo speciali creano figli. Qui se root non è speciale ma ricorrente,
        // creiamo figli con parent = root.
        materializeChildOccurrences(seriesRoot, isoDay, end, weeks);
    }

    private void materializeChildOccurrences(Event parent, Integer isoDay, LocalDate end, int weeksAhead) {
        if (isoDay == null) {
            createChildIfAbsent(parent, parent.getData());
            return;
        }
        DayOfWeek dow = DayOfWeek.of(isoDay);
        LocalTime time = parent.getData().toLocalTime();
        LocalDate start = parent.getData().toLocalDate();
        LocalDate cursor = start;
        if (cursor.getDayOfWeek() != dow) {
            cursor = cursor.with(java.time.temporal.TemporalAdjusters.nextOrSame(dow));
        }

        LocalDate horizon = LocalDate.now().plusWeeks(weeksAhead);
        LocalDate limit = end != null && end.isBefore(horizon) ? end : horizon;
        if (end != null && end.isBefore(cursor)) {
            return;
        }

        int guard = 0;
        while (!cursor.isAfter(limit) && guard < 200) {
            LocalDateTime when = LocalDateTime.of(cursor, time);
            if (when.isAfter(LocalDateTime.now().minusMinutes(1))) {
                createChildIfAbsent(parent, when);
            }
            cursor = cursor.plusWeeks(1);
            guard++;
        }
    }

    private void createChildIfAbsent(Event parent, LocalDateTime when) {
        if (eventRepository.existsByParentEventIdAndData(parent.getId(), when)) {
            return;
        }
        // Evita duplicare la stessa data dello speciale come figlio se uguale e già usata solo come template:
        // creiamo comunque il figlio prenotabile alla stessa data.
        Event child = new Event();
        child.setTitolo(parent.getTitolo());
        child.setDescrizione(parent.getDescrizione());
        child.setData(when);
        child.setPostiTotali(parent.getPostiTotali());
        child.setSpeciale(false);
        child.setRicorrente(false);
        child.setParentEvent(parent);
        child.setGiornoSettimana(null);
        child.setDataFine(null);
        // locandina ereditata in EventResponse dal parent
        eventRepository.save(child);
    }

    private void syncChildrenMetadata(Event parent) {
        List<Event> children = eventRepository.findByParentEventIdOrderByDataAsc(parent.getId());
        for (Event child : children) {
            child.setTitolo(parent.getTitolo());
            child.setDescrizione(parent.getDescrizione());
            child.setPostiTotali(parent.getPostiTotali());
            eventRepository.save(child);
        }
    }

    private LocalDateTime resolveFirstOccurrence(LocalDateTime requested, boolean ricorrente, Integer isoDay) {
        if (requested == null) {
            throw new CornerPubException("La data è obbligatoria");
        }
        if (!ricorrente || isoDay == null) {
            if (!requested.isAfter(LocalDateTime.now())) {
                throw new CornerPubException("La data deve essere nel futuro");
            }
            return requested;
        }
        DayOfWeek dow = DayOfWeek.of(isoDay);
        LocalDate d = requested.toLocalDate();
        if (d.getDayOfWeek() != dow) {
            d = d.with(java.time.temporal.TemporalAdjusters.nextOrSame(dow));
        }
        LocalDateTime candidate = LocalDateTime.of(d, requested.toLocalTime());
        if (!candidate.isAfter(LocalDateTime.now())) {
            candidate = candidate.plusWeeks(1);
        }
        return candidate;
    }

    private EventResponse toResponse(Event event) {
        long totale = registrationRepository.countByEventId(event.getId());
        EventResponse response = new EventResponse(event, totale);
        if (event.getParentEvent() == null) {
            response.setFigliCount((int) eventRepository.countByParentEventId(event.getId()));
        }
        return response;
    }
}
