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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserService userService;
    private final EventRepository eventRepository;
    private final EventRegistrationService eventRegistrationService;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        User user = userService.findOrCreate(request.getName(), request.getPhone());
        LocalDate date = LocalDate.parse(request.getDate());

        if (reservationRepository.findByUser_PhoneAndDate(user.getPhone(), date).isPresent()) {
            throw new ReservationAlreadyExistsException(user.getPhone(), request.getDate());
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setDate(date);
        reservation.setTime(LocalTime.parse(request.getTime()));
        reservation.setPeople(request.getPeople());
        reservation.setNote(request.getNote());

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse createAdminReservation(ReservationRequest request) {
        if (request.getEventId() != null) {
            // Crea sia la registrazione all'evento che la prenotazione
            EventRegistrationRequest eventRegRequest = new EventRegistrationRequest();
            eventRegRequest.setName(request.getName());
            eventRegRequest.setPhone(request.getPhone());

            eventRegistrationService.register(request.getEventId(), eventRegRequest);

            // Crea anche la prenotazione associata all'evento
            User user = userService.findOrCreate(request.getName(), request.getPhone());
            Reservation reservation = new Reservation();
            reservation.setUser(user);
            reservation.setDate(LocalDate.parse(request.getDate()));
            reservation.setTime(LocalTime.parse(request.getTime()));
            reservation.setPeople(request.getPeople());
            reservation.setNote(request.getNote());
            reservation.setEvent(eventRepository.findById(request.getEventId()).orElse(null));

            return toResponse(reservationRepository.save(reservation));
        } else {
            // Prenotazione normale
            return createReservation(request);
        }
    }

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

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("ID", id.toString()));

        User user = userService.findOrCreate(request.getName(), request.getPhone());
        reservation.setUser(user);
        reservation.setDate(LocalDate.parse(request.getDate()));
        reservation.setTime(LocalTime.parse(request.getTime()));
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
                .findByUser_PhoneAndDate(phone, date)
                .orElseThrow(() -> new ReservationNotFoundException(phone, dateString));
        reservationRepository.delete(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(String phone, String dateString) {
        LocalDate date = parseDate(dateString);
        Reservation reservation = reservationRepository
                .findByUser_PhoneAndDate(phone, date)
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

    @Transactional(readOnly = true)
    public List<String> getAvailableTimes(String dateString) {
        LocalDate date = parseDate(dateString);
        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) {
            return List.of();
        }

        Map<String, Long> countMap = reservationRepository.findAllByDate(date).stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTime().toString(),
                        Collectors.counting()
                ));

        List<String> available = new ArrayList<>();
        LocalTime start = LocalTime.of(20, 0);
        LocalTime end = LocalTime.of(23, 0);
        LocalTime now = LocalTime.now();

        while (!start.isAfter(end)) {
            String timeStr = start.toString();

            if (date.equals(today) && start.isBefore(now)) {
                start = start.plusMinutes(30);
                continue;
            }

            if (countMap.getOrDefault(timeStr, 0L) < 12) {
                available.add(timeStr);
            }
            start = start.plusMinutes(30);
        }

        return available;
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getFutureReservationsByPhone(String phone) {
        return reservationRepository.findByUser_PhoneAndDate(phone, LocalDate.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private LocalDate parseDate(String dateString) {
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ISO_LOCAL_DATE,               // yyyy-MM-dd
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),      // dd-MM-yyyy
                DateTimeFormatter.ofPattern("MM/dd/yyyy")       // opzionale se serve
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateString, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new BadRequestException("Formato data non valido. Usa yyyy-MM-dd o dd-MM-yyyy");
    }


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

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsWithEvents(LocalDate date) {
        return reservationRepository.findAllByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getTodayReservations() {
        return reservationRepository.findAllFromToday(LocalDate.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllUserReservations(String phone) {
        // Prenotazioni normali
        List<ReservationResponse> reservations = reservationRepository
                .findAllByUser_Phone(phone)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Prenotazioni derivate da EventRegistration (se non hai Reservation create anche lì)
        List<ReservationResponse> eventReservations = eventRegistrationService
                .getRegistrationsByPhone(phone)
                .stream()
                .map(reg -> {
                    ReservationResponse resp = new ReservationResponse();
                    resp.setId(reg.getId());
                    resp.setName(reg.getName());
                    resp.setPhone(reg.getPhone());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                    LocalDateTime dateTime = LocalDateTime.parse(reg.getEvent().getData(), formatter);

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

        // Unisco tutto
        reservations.addAll(eventReservations);
        return reservations;
    }

}