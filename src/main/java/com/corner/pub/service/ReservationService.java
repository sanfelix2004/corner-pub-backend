package com.corner.pub.service;

import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.exception.badrequest.BadRequestException;
import com.corner.pub.exception.conflictexception.ReservationAlreadyExistsException;
import com.corner.pub.exception.resourcenotfound.ReservationNotFoundException;
import com.corner.pub.model.Reservation;
import com.corner.pub.model.User;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FMT_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter EVENT_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ReservationRepository reservationRepository;
    private final UserService userService;
    private final EventRepository eventRepository;
    private final EventRegistrationService eventRegistrationService;

    // Sala: 12 tavoli numerati 1..12, ~70 posti totali
    private static final int MAX_TABLES = 12;
    private static final int MAX_SEATS = 70;
    // Limite ragionevole per singola prenotazione (1 tavolo)
    private static final int MAX_PEOPLE_PER_RESERVATION = 12;

    // -----------------------------
    //           CAPACITY HELPERS
    // -----------------------------

    private List<Reservation> getReservationsAt(LocalDate date, LocalTime time) {
        return reservationRepository.findAllByDate(date).stream()
                .filter(r -> Objects.equals(r.getTime(), time))
                .collect(Collectors.toList());
    }

    private void enforceCapacity(LocalDate date, LocalTime time, int people, Long ignoreReservationId) {
        if (people <= 0) {
            throw new BadRequestException("Il numero di persone deve essere maggiore di 0");
        }
        if (people > MAX_PEOPLE_PER_RESERVATION) {
            throw new BadRequestException("Prenotazione troppo numerosa per un singolo tavolo (max " + MAX_PEOPLE_PER_RESERVATION + ")");
        }
        List<Reservation> atSlot = getReservationsAt(date, time);
        // escludi la prenotazione che stiamo aggiornando (se presente)
        if (ignoreReservationId != null) {
            atSlot = atSlot.stream()
                    .filter(r -> !Objects.equals(r.getId(), ignoreReservationId))
                    .collect(Collectors.toList());
        }
        int currentReservations = atSlot.size();
        int currentPeople = atSlot.stream().mapToInt(Reservation::getPeople).sum();

        if (currentReservations >= MAX_TABLES) {
            throw new BadRequestException("Nessun tavolo disponibile a quest'ora (" + time + ")");
        }
        if (currentPeople + people > MAX_SEATS) {
            throw new BadRequestException("Capienza massima della sala superata per questo orario (" + time + ")");
        }
    }

    private String autoAssignTable(LocalDate date, LocalTime time, Long ignoreReservationId) {
        Set<String> used = getReservationsAt(date, time).stream()
                .filter(r -> ignoreReservationId == null || !Objects.equals(r.getId(), ignoreReservationId))
                .map(Reservation::getTableNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (int i = 1; i <= MAX_TABLES; i++) {
            String tn = String.valueOf(i);
            if (!used.contains(tn)) return tn;
        }
        return null; // nessun tavolo libero (dovrebbe essere intercettato da enforceCapacity)
    }

    // -----------------------------
    //           CREATE / UPDATE
    // -----------------------------

    @Transactional(readOnly = true)
    public List<ReservationResponse> getActiveReservations() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return reservationRepository.findAllFromToday(today).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        User user = userService.findOrCreate(request.getName(), request.getPhone());
        LocalDate date = parseDate(request.getDate());
        LocalTime time = parseTime(request.getTime());

        reservationRepository.findByUserPhoneAndDate(user.getPhone(), date)
                .ifPresent(r -> { throw new ReservationAlreadyExistsException(user.getPhone(), request.getDate()); });

        enforceCapacity(date, time, request.getPeople(), null);

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setDate(date);
        reservation.setTime(time);
        reservation.setPeople(request.getPeople());
        reservation.setNote(request.getNote());

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse createAdminReservation(ReservationRequest request) {
        if (request.getEventId() != null) {
            // Registra l'utente all'evento
            EventRegistrationRequest eventRegRequest = new EventRegistrationRequest();
            eventRegRequest.setName(request.getName());
            eventRegRequest.setPhone(request.getPhone());
            eventRegistrationService.register(request.getEventId(), eventRegRequest);

            // Crea anche la prenotazione associata
            User user = userService.findOrCreate(request.getName(), request.getPhone());
            LocalDate date = parseDate(request.getDate());
            LocalTime time = parseTime(request.getTime());

            enforceCapacity(date, time, request.getPeople(), null);

            Reservation reservation = new Reservation();
            reservation.setUser(user);
            reservation.setDate(date);
            reservation.setTime(time);
            reservation.setPeople(request.getPeople());
            reservation.setNote(request.getNote());
            reservation.setEvent(eventRepository.findById(request.getEventId()).orElse(null));

            return toResponse(reservationRepository.save(reservation));
        }
        // Prenotazione normale
        return createReservation(request);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("ID", id.toString()));

        User user = userService.findOrCreate(request.getName(), request.getPhone());
        reservation.setUser(user);
        LocalDate date = parseDate(request.getDate());
        LocalTime time = parseTime(request.getTime());

        enforceCapacity(date, time, request.getPeople(), reservation.getId());

        reservation.setDate(date);
        reservation.setTime(time);
        reservation.setPeople(request.getPeople());
        reservation.setNote(request.getNote());

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse assignTable(Long id, String tableNumber) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("ID", id.toString()));

        if (tableNumber == null || tableNumber.isBlank()) {
            throw new BadRequestException("Numero tavolo mancante");
        }
        int tn;
        try { tn = Integer.parseInt(tableNumber.trim()); } catch (NumberFormatException e) { throw new BadRequestException("Numero tavolo non valido"); }
        if (tn < 1 || tn > MAX_TABLES) {
            throw new BadRequestException("Il tavolo deve essere compreso tra 1 e " + MAX_TABLES);
        }
        // verifica non occupato nello stesso slot
        boolean taken = getReservationsAt(reservation.getDate(), reservation.getTime()).stream()
                .anyMatch(r -> !Objects.equals(r.getId(), reservation.getId()) && String.valueOf(tn).equals(r.getTableNumber()));
        if (taken) {
            throw new BadRequestException("Tavolo già occupato per questo orario");
        }

        reservation.setTableNumber(String.valueOf(tn));
        return toResponse(reservationRepository.save(reservation));
    }


    @Transactional
    public void deleteById(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ReservationNotFoundException("ID", id.toString());
        }
        reservationRepository.deleteById(id);
    }

    @Transactional
    public void deleteReservationByPhoneAndDate(String phone, String dateString) {
        LocalDate date = parseDate(dateString);
        Reservation reservation = reservationRepository
                .findByUserPhoneAndDate(phone, date)
                .orElseThrow(() -> new ReservationNotFoundException(phone, dateString));
        reservationRepository.delete(reservation);
    }

    // -----------------------------
    //             READ
    // -----------------------------

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("ID", id.toString()));
        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(String phone, String dateString) {
        LocalDate date = parseDate(dateString);
        Reservation reservation = reservationRepository
                .findByUserPhoneAndDate(phone, date)
                .orElseThrow(() -> new ReservationNotFoundException(phone, dateString));
        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByPhone(String phone) {
        return reservationRepository.findAllWithEventByUserPhone(phone).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByDate(String dateString) {
        LocalDate date = parseDate(dateString);
        return reservationRepository.findAllByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Ritorna gli slot disponibili (stessa logica tua). Da oggi in poi.
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableTimes(String dateString) {
        LocalDate date = parseDate(dateString);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (date.isBefore(today)) {
            return List.of();
        }

        List<String> available = new ArrayList<>();
        LocalTime start = LocalTime.of(20, 0);
        LocalTime end = LocalTime.of(23, 0);
        LocalTime now = LocalTime.now(ZoneId.systemDefault());

        while (!start.isAfter(end)) {
            String timeStr = start.toString();

            if (date.equals(today) && start.isBefore(now)) {
                start = start.plusMinutes(30);
                continue;
            }

            List<Reservation> atSlot = getReservationsAt(date, start);
            int reservationsCount = atSlot.size();
            int peopleCount = atSlot.stream().mapToInt(Reservation::getPeople).sum();

            if (reservationsCount < MAX_TABLES && peopleCount < MAX_SEATS) {
                available.add(timeStr);
            }
            start = start.plusMinutes(30);
        }

        return available;
    }

    /**
     * La tua vecchia implementazione prendeva solo "oggi".
     * Mantengo il senso logico ma la rendo utile (future >= today per quell'utente).
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getFutureReservationsByPhone(String phone) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return reservationRepository.findAllWithEventByUserPhone(phone).stream()
                .filter(r -> !r.getDate().isBefore(today))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsWithEvents(LocalDate date) {
        return reservationRepository.findAllByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * La tua getTodayReservations usava "from today". Mantengo l’attuale comportamento:
     * “da oggi in poi”, come nel repository (non solo oggi).
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getTodayReservations() {
        return reservationRepository.findAllFromToday(LocalDate.now(ZoneId.systemDefault())).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -----------------------------
    //          MAPPING
    // -----------------------------

    public ReservationResponse toResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setName(reservation.getUser().getName());
        response.setPhone(reservation.getUser().getPhone());
        response.setDate(reservation.getDate());
        response.setTime(reservation.getTime());
        response.setPeople(reservation.getPeople());
        response.setNote(reservation.getNote());
        response.setEventId(reservation.getEvent() != null ? reservation.getEvent().getId() : null);
        response.setIsEventRegistration(reservation.getEvent() != null);
        response.setTableNumber(reservation.getTableNumber());
        return response;
    }

    // -----------------------------
    //       PARSING & HELPERS
    // -----------------------------

    private static String clean(String in) {
        if (in == null) return null;
        String s = in.replace('\u00A0', ' ')  // NBSP -> space
                .replace("\u200B", "")   // zero width space
                .trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private LocalDate parseDate(String raw) {
        String dateString = clean(raw);
        log.info("🟡 DEBUG PARSE DATE - Input received: '{}'", dateString);

        // 1) esatto yyyy-MM-dd
        try {
            LocalDate parsed = LocalDate.parse(dateString, DATE_FMT);
            log.info("✅ DEBUG PARSE DATE - Parsed with ISO_LOCAL_DATE: {}", parsed);
            return parsed;
        } catch (DateTimeParseException e) {
            log.warn("❌ DEBUG PARSE DATE - Not ISO_LOCAL_DATE: {}", e.getMessage());
        }

        // 2) se arriva ISO datetime (2025-08-26T00:00:00Z/±offset) o con spazio, prendo i primi 10 char
        if (dateString != null && dateString.length() >= 10) {
            String head10 = dateString.substring(0, 10);
            if (head10.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    LocalDate parsed = LocalDate.parse(head10, DATE_FMT);
                    log.info("✅ DEBUG PARSE DATE - Parsed head(10): {}", parsed);
                    return parsed;
                } catch (DateTimeParseException ignored) { /* no-op */ }
            }
        }

        log.error("🔥 DEBUG PARSE DATE - ALL FORMATTERS FAILED! value='{}' len={}",
                dateString, dateString == null ? "null" : dateString.length());
        throw new BadRequestException("Formato data non valido. Usa yyyy-MM-dd. Valore ricevuto: '" + dateString + "'");
    }

    private LocalTime parseTime(String raw) {
        String s = clean(raw);
        if (s == null || s.isBlank()) {
            throw new BadRequestException("Orario mancante o non valido. Usa HH:mm. Valore ricevuto: '" + raw + "'");
        }
        // prima HH:mm, poi HH:mm:ss come fallback
        try {
            return LocalTime.parse(s, TIME_FMT);
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(s, TIME_FMT_SECONDS);
            } catch (DateTimeParseException e2) {
                throw new BadRequestException("Formato ora non valido. Usa HH:mm. Valore ricevuto: '" + s + "'");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllUserReservations(String phone) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        // Prenotazioni “normali”
        List<ReservationResponse> reservations = reservationRepository
                .findAllByUserPhone(phone)
                .stream()
                .filter(r -> !r.getDate().isBefore(today)) // 🔹 solo oggi/futuro
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Prenotazioni da eventi
        List<ReservationResponse> eventReservations = eventRegistrationService
                .getRegistrationsByPhone(phone)
                .stream()
                .map(reg -> {
                    ReservationResponse resp = new ReservationResponse();
                    resp.setId(reg.getId());
                    resp.setName(reg.getName());
                    resp.setPhone(reg.getPhone());

                    LocalDateTime dateTime = LocalDateTime.parse(reg.getEvent().getData(), EVENT_DT_FMT);
                    resp.setDate(dateTime.toLocalDate());
                    resp.setTime(dateTime.toLocalTime());
                    resp.setPeople(reg.getPartecipanti());
                    resp.setNote(reg.getNote());
                    resp.setIsEventRegistration(true);
                    resp.setEvent(reg.getEvent());
                    resp.setEventId(reg.getEvent().getId());
                    return resp;
                })
                .filter(r -> !r.getDate().isBefore(today)) // 🔹 anche sugli eventi
                .collect(Collectors.toList());

        reservations.addAll(eventReservations);
        return reservations;
    }

}
