package com.corner.pub.service;

import com.corner.pub.dto.request.EventRequest;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.model.Event;
import com.corner.pub.repository.EventRegistrationRepository;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.repository.SpecialeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
    private static final ZoneId ROME = ZoneId.of("Europe/Rome");

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final StorageService storageService;
    private final SpecialeRepository specialeRepository;

    @Transactional
    public EventResponse createEvent(EventRequest request, MultipartFile poster) {
        boolean speciale = Boolean.TRUE.equals(request.getSpeciale());
        boolean ricorrente = Boolean.TRUE.equals(request.getRicorrente());
        String giorni = resolveGiorni(request, null);

        if (ricorrente && (giorni == null || giorni.isBlank()) && request.getGiornoSettimana() == null) {
            throw new CornerPubException("Per un evento ricorrente scegli almeno un giorno della settimana");
        }
        if (ricorrente && (giorni == null || giorni.isBlank()) && request.getGiornoSettimana() != null) {
            giorni = String.valueOf(request.getGiornoSettimana());
        }

        Integer singleDay = firstDay(giorni, request.getGiornoSettimana());
        // Ricorrenza virtuale = Speciali (CSV giorni + eventuale specialeId). Run Club e
        // l'admin "Eventi" con solo giornoSettimana restano sul modello a figli materializzati.
        boolean csvGiorni = request.getGiorniSettimana() != null && !request.getGiorniSettimana().isBlank();
        boolean virtualRecurrence = ricorrente
                && !Boolean.TRUE.equals(request.getRunClub())
                && (csvGiorni || request.getSpecialeId() != null);

        LocalDateTime firstDate = virtualRecurrence
                ? RecurrenceSupport.firstOccurrence(
                        request.getData() != null ? request.getData().toLocalDate() : LocalDate.now(ROME),
                        request.getData() != null ? request.getData().toLocalTime() : LocalTime.of(20, 0),
                        giorni)
                : resolveFirstOccurrence(request.getData(), ricorrente, singleDay);

        Event event = new Event();
        event.setTitolo(request.getTitolo());
        event.setDescrizione(request.getDescrizione());
        event.setData(firstDate);
        event.setPostiTotali(request.getPostiTotali());
        event.setSpeciale(speciale);
        event.setRicorrente(ricorrente);
        event.setGiornoSettimana(ricorrente ? singleDay : null);
        event.setGiorniSettimana(ricorrente ? RecurrenceSupport.serialize(giorni) : null);
        event.setDataFine(ricorrente ? request.getDataFine() : null);
        event.setSpecialeId(request.getSpecialeId());
        event.setRunClub(Boolean.TRUE.equals(request.getRunClub()));
        event.setLuogo(trimToNull(request.getLuogo()));
        event.setDistanza(trimToNull(request.getDistanza()));
        event.setRitmo(trimToNull(request.getRitmo()));
        event.setQuotaEuro(request.getQuotaEuro() == null ? 0 : request.getQuotaEuro());
        event.setAnnullata(false);

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
        } else if (ricorrente && !speciale && !virtualRecurrence && saved.getSpecialeId() == null) {
            // Serie materializzata (es. Run Club via EventService): crea figli.
            materializeOccurrences(saved, saved.getGiornoSettimana(), saved.getDataFine(), OCCURRENCE_HORIZON_WEEKS);
        }

        return toResponse(saved, null);
    }

    @Transactional
    public EventResponse copyPosterFromUrl(Long eventId, String sourceUrl) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return toResponse(event, null);
        }
        try {
            String relative = storageService.copyTo(sourceUrl, "eventi", String.valueOf(event.getId()));
            if (relative != null) {
                event.setPosterUrl(storageService.publicUrl(relative));
                event.setPosterPublicId(relative);
                event = eventRepository.save(event);
            }
        } catch (Exception e) {
            log.warn("Copia locandina da speciale non riuscita: {}", e.getMessage());
        }
        return toResponse(event, null);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request, MultipartFile poster) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        boolean ricorrente = Boolean.TRUE.equals(request.getRicorrente());
        String giorni = resolveGiorni(request, event);
        if (ricorrente && (giorni == null || giorni.isBlank()) && request.getGiornoSettimana() == null) {
            throw new CornerPubException("Per un evento ricorrente scegli almeno un giorno della settimana");
        }
        if (ricorrente && (giorni == null || giorni.isBlank()) && request.getGiornoSettimana() != null) {
            giorni = String.valueOf(request.getGiornoSettimana());
        }

        Integer singleDay = firstDay(giorni, request.getGiornoSettimana());
        boolean csvGiorni = request.getGiorniSettimana() != null && !request.getGiorniSettimana().isBlank();
        boolean linkedSpeciale = request.getSpecialeId() != null || event.getSpecialeId() != null;
        boolean virtualRecurrence = ricorrente
                && !Boolean.TRUE.equals(request.getRunClub())
                && !event.isRunClub()
                && (csvGiorni || linkedSpeciale);

        LocalDateTime firstDate = virtualRecurrence
                ? RecurrenceSupport.firstOccurrence(
                        request.getData() != null ? request.getData().toLocalDate() : event.getData().toLocalDate(),
                        request.getData() != null ? request.getData().toLocalTime() : event.getData().toLocalTime(),
                        giorni)
                : resolveFirstOccurrence(request.getData(), ricorrente, singleDay);

        event.setTitolo(request.getTitolo());
        event.setDescrizione(request.getDescrizione());
        event.setData(firstDate);
        event.setPostiTotali(request.getPostiTotali());
        if (request.getSpeciale() != null) {
            event.setSpeciale(request.getSpeciale());
        }
        event.setRicorrente(ricorrente);
        event.setGiornoSettimana(ricorrente ? singleDay : null);
        event.setGiorniSettimana(ricorrente ? RecurrenceSupport.serialize(giorni) : null);
        event.setDataFine(ricorrente ? request.getDataFine() : null);
        if (request.getSpecialeId() != null) {
            event.setSpecialeId(request.getSpecialeId());
        }
        if (request.getRunClub() != null) {
            event.setRunClub(request.getRunClub());
        }
        if (request.getLuogo() != null) {
            event.setLuogo(trimToNull(request.getLuogo()));
        }
        if (request.getDistanza() != null) {
            event.setDistanza(trimToNull(request.getDistanza()));
        }
        if (request.getRitmo() != null) {
            event.setRitmo(trimToNull(request.getRitmo()));
        }
        if (request.getQuotaEuro() != null) {
            event.setQuotaEuro(request.getQuotaEuro());
        }

        if (poster != null && !poster.isEmpty()) {
            replaceStoredPoster(event, poster);
        }

        Event updated = eventRepository.save(event);

        if (updated.isSpeciale() && eventRepository.countByParentEventId(updated.getId()) > 0) {
            syncChildrenMetadata(updated);
            ensureChildrenHorizon(updated, request);
        } else if (updated.isRicorrente()
                && updated.getParentEvent() == null
                && !virtualRecurrence
                && updated.getSpecialeId() == null
                && !updated.isSpeciale()) {
            materializeOccurrences(updated, updated.getGiornoSettimana(), updated.getDataFine(), OCCURRENCE_HORIZON_WEEKS);
        }

        return toResponse(updated, null);
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
        return toResponse(updated, null);
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
        return toResponse(updated, null);
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

        List<Event> children = eventRepository.findByParentEventIdOrderByDataAsc(eventId);
        for (Event child : children) {
            storageService.deleteOwned(child.getPosterUrl(), "eventi", String.valueOf(child.getId()));
            storageService.delete(child.getPosterPublicId());
            registrationRepository.deleteByEventId(child.getId());
            eventRepository.delete(child);
        }

        storageService.deleteOwned(event.getPosterUrl(), "eventi", String.valueOf(eventId));
        storageService.delete(event.getPosterPublicId());

        specialeRepository.findByEventoId(eventId).ifPresent(speciale -> {
            speciale.setEventoId(null);
            specialeRepository.save(speciale);
        });

        registrationRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

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
                .filter(event -> !event.isRunClub())
                .map(event -> toResponse(event, null))
                .collect(Collectors.toList());
    }

    /**
     * Eventi prenotabili dal pubblico.
     * - Ricorrenza virtuale (giorni_settimana / speciale collegato): una riga con prossimeDate.
     * - Serie materializzata: nasconde i template con figli, mostra le date figlie future.
     */
    @Transactional
    public List<EventResponse> listBookablePublic(LocalDate filterDate) {
        ensureRecurringHorizons();

        LocalDateTime now = LocalDateTime.now(ROME);
        LocalDate today = LocalDate.now(ROME);

        List<Event> candidates;
        if (filterDate != null) {
            candidates = eventRepository.findBookable(now, today).stream()
                    .filter(event -> RecurrenceSupport.isValidOccurrence(event, filterDate))
                    .toList();
        } else {
            candidates = eventRepository.findBookable(now, today);
        }

        Set<Long> parentsWithChildren = new HashSet<>();
        for (Event e : candidates) {
            if (e.getParentEvent() != null) {
                parentsWithChildren.add(e.getParentEvent().getId());
            }
        }
        for (Event e : eventRepository.findBySpecialeTrueAndRicorrenteTrue()) {
            if (eventRepository.countByParentEventId(e.getId()) > 0) {
                parentsWithChildren.add(e.getId());
            }
        }

        List<EventResponse> out = new ArrayList<>();
        for (Event event : candidates) {
            if (event.isRunClub()) {
                continue;
            }
            if (event.isSpeciale() && parentsWithChildren.contains(event.getId())) {
                continue;
            }
            // Figli materializzati: solo se la data è ancora futura (o filtra)
            if (event.getParentEvent() != null && filterDate == null
                    && !event.getData().isAfter(now)) {
                continue;
            }
            Event detailed = eventRepository.findDetailedById(event.getId()).orElse(event);
            out.add(toResponse(detailed, filterDate));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long eventId) {
        Event event = eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));
        return toResponse(event, null);
    }

    @Transactional(readOnly = true)
    public EventResponse getPublic(Long eventId, LocalDate occurrence) {
        Event event = eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));
        if (occurrence != null && !RecurrenceSupport.isValidOccurrence(event, occurrence)) {
            throw new CornerPubException("La data scelta non è valida per questo evento");
        }
        return toResponse(event, occurrence);
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
            createChildIfAbsent(special, special.getData());
            return;
        }
        materializeChildOccurrences(special, day, end, OCCURRENCE_HORIZON_WEEKS);
    }

    private void materializeOccurrences(Event seriesRoot, Integer isoDay, LocalDate end, int weeks) {
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

        LocalDate horizon = LocalDate.now(ROME).plusWeeks(weeksAhead);
        LocalDate limit = end != null && end.isBefore(horizon) ? end : horizon;
        if (end != null && end.isBefore(cursor)) {
            return;
        }

        int guard = 0;
        while (!cursor.isAfter(limit) && guard < 200) {
            LocalDateTime when = LocalDateTime.of(cursor, time);
            if (when.isAfter(LocalDateTime.now(ROME).minusMinutes(1))) {
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
        Event child = new Event();
        child.setTitolo(parent.getTitolo());
        child.setDescrizione(parent.getDescrizione());
        child.setData(when);
        child.setPostiTotali(parent.getPostiTotali());
        child.setSpeciale(false);
        child.setRicorrente(false);
        child.setParentEvent(parent);
        child.setGiornoSettimana(null);
        child.setGiorniSettimana(null);
        child.setDataFine(null);
        child.setSpecialeId(parent.getSpecialeId());
        child.setRunClub(parent.isRunClub());
        child.setLuogo(parent.getLuogo());
        child.setDistanza(parent.getDistanza());
        child.setRitmo(parent.getRitmo());
        child.setQuotaEuro(parent.getQuotaEuro());
        child.setAnnullata(false);
        eventRepository.save(child);
    }

    private void syncChildrenMetadata(Event parent) {
        List<Event> children = eventRepository.findByParentEventIdOrderByDataAsc(parent.getId());
        for (Event child : children) {
            child.setTitolo(parent.getTitolo());
            child.setDescrizione(parent.getDescrizione());
            child.setPostiTotali(parent.getPostiTotali());
            child.setRunClub(parent.isRunClub());
            child.setLuogo(parent.getLuogo());
            child.setDistanza(parent.getDistanza());
            child.setRitmo(parent.getRitmo());
            child.setQuotaEuro(parent.getQuotaEuro());
            eventRepository.save(child);
        }
    }

    private LocalDateTime resolveFirstOccurrence(LocalDateTime requested, boolean ricorrente, Integer isoDay) {
        if (requested == null) {
            throw new CornerPubException("La data è obbligatoria");
        }
        if (!ricorrente || isoDay == null) {
            if (!requested.isAfter(LocalDateTime.now(ROME))) {
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
        if (!candidate.isAfter(LocalDateTime.now(ROME))) {
            candidate = candidate.plusWeeks(1);
        }
        return candidate;
    }

    private static String resolveGiorni(EventRequest request, Event existing) {
        if (request.getGiorniSettimana() != null && !request.getGiorniSettimana().isBlank()) {
            return RecurrenceSupport.serialize(request.getGiorniSettimana());
        }
        if (request.getGiornoSettimana() != null) {
            return String.valueOf(request.getGiornoSettimana());
        }
        if (existing != null) {
            return existing.effectiveGiorniSettimana();
        }
        return null;
    }

    private static Integer firstDay(String giorni, Integer fallback) {
        List<DayOfWeek> days = RecurrenceSupport.parseDays(giorni);
        if (!days.isEmpty()) {
            return days.get(0).getValue();
        }
        return fallback;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private EventResponse toResponse(Event event, LocalDate occurrence) {
        LocalDate occ = occurrence;
        if (occ == null && event.isRicorrente()
                && event.effectiveGiorniSettimana() != null
                && eventRepository.countByParentEventId(event.getId()) == 0) {
            List<LocalDate> upcoming = RecurrenceSupport.upcoming(event, RecurrenceSupport.UPCOMING_LIMIT);
            occ = upcoming.isEmpty() ? null : upcoming.get(0);
        }

        long totale;
        if (occ != null && event.isRicorrente()
                && event.effectiveGiorniSettimana() != null
                && eventRepository.countByParentEventId(event.getId()) == 0) {
            totale = registrationRepository
                    .sumPartecipantiByEventIdAndOccurrenceDate(event.getId(), occ)
                    .orElse(0L);
        } else {
            totale = registrationRepository.countByEventId(event.getId());
        }

        EventResponse response = new EventResponse(event, totale);
        if (event.getParentEvent() == null) {
            response.setFigliCount((int) eventRepository.countByParentEventId(event.getId()));
        }
        if (occ != null && event.isRicorrente()) {
            response.setData(RecurrenceSupport.atEventTime(event, occ).toString());
        }
        return response;
    }
}
