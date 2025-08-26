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

    // -----------------------------
    //           CREATE / UPDATE
    // -----------------------------

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        User user = userService.findOrCreate(request.getName(), request.getPhone());
        LocalDate date = parseDate(request.getDate());

        reservationRepository.findByUserPhoneAndDate(user.getPhone(), date)
                .ifPresent(r -> { throw new ReservationAlreadyExistsException(user.getPhone(), request.getDate()); });

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setDate(date);
        reservation.setTime(parseTime(request.getTime()));
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
            Reservation reservation = new Reservation();
            reservation.setUser(user);
            reservation.setDate(parseDate(request.getDate()));
            reservation.setTime(parseTime(request.getTime()));
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
        reservation.setDate(parseDate(request.getDate()));
        reservation.setTime(parseTime(request.getTime()));
        reservation.setPeople(request.getPeople());
        reservation.setNote(request.getNote());

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

        Map<String, Long> countMap = reservationRepository.findAllByDate(date).stream()
                .collect(Collectors.groupingBy(r -> r.getTime().toString(), Collectors.counting()));

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

            // capienza 12 come nel tuo codice
            if (countMap.getOrDefault(timeStr, 0L) < 12L) {
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
        // Prenotazioni “normali”
        List<ReservationResponse> reservations = reservationRepository
                .findAllByUserPhone(phone)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Prenotazioni derivate da EventRegistration
        List<ReservationResponse> eventReservations = eventRegistrationService
                .getRegistrationsByPhone(phone)
                .stream()
                .map(reg -> {
                    ReservationResponse resp = new ReservationResponse();
                    resp.setId(reg.getId());
                    resp.setName(reg.getName());
                    resp.setPhone(reg.getPhone());

                    LocalDateTime dateTime;
                    try {
                        dateTime = LocalDateTime.parse(reg.getEvent().getData(), EVENT_DT_FMT);
                    } catch (DateTimeParseException e) {
                        // fallback difensivo: se cambia formato in futuro
                        throw new BadRequestException("Formato data/ora evento non valido: '" + reg.getEvent().getData() + "'");
                    }

                    resp.setDate(dateTime.toLocalDate());
                    resp.setTime(dateTime.toLocalTime());
                    resp.setPeople(reg.getPartecipanti());
                    resp.setNote(reg.getNote());
                    resp.setIsEventRegistration(true);
                    resp.setEvent(reg.getEvent());
                    resp.setEventId(reg.getEvent().getId());
                    return resp;
                })
                .collect(Collectors.toList());

        reservations.addAll(eventReservations);
        return reservations;
    }
}
