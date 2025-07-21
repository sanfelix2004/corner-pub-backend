package com.corner.pub.service;

import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.exception.badrequest.BadRequestException;
import com.corner.pub.exception.conflictexception.ReservationAlreadyExistsException;
import com.corner.pub.exception.resourcenotfound.ReservationNotFoundException;
import com.corner.pub.model.Reservation;
import com.corner.pub.model.User;
import com.corner.pub.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserService userService;

    // ✅ Crea una nuova prenotazione (con check duplicato)
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

    // ✅ Elenco completo delle prenotazioni
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // ✅ Trova una prenotazione specifica (per ID)
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("ID", id.toString()));
        return toResponse(reservation);
    }

    // ✅ Modifica una prenotazione esistente (per ID)
    public ReservationResponse updateReservation(Long id, ReservationRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("ID", id.toString()));

        // aggiorna l'utente o crealo se nuovo
        User user = userService.findOrCreate(request.getName(), request.getPhone());
        reservation.setUser(user);
        reservation.setDate(LocalDate.parse(request.getDate()));
        reservation.setTime(LocalTime.parse(request.getTime()));
        reservation.setPeople(request.getPeople());
        reservation.setNote(request.getNote());

        return toResponse(reservationRepository.save(reservation));
    }

    // ✅ Cancella prenotazione per ID
    public void deleteById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("ID", id.toString()));
        reservationRepository.delete(reservation);
    }

    // ✅ Cancella prenotazione per telefono + data
    public void deleteReservationByPhoneAndDate(String phone, String dateString) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato data non valido. Usa yyyy-MM-dd");
        }
        Reservation reservation = reservationRepository
                .findByUser_PhoneAndDate(phone, date)
                .orElseThrow(() -> new ReservationNotFoundException(phone, dateString));
        reservationRepository.delete(reservation);
    }
    public void deleteReservation(String phone, String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        Reservation reservation = reservationRepository
                .findByUser_PhoneAndDate(phone, date)
                .orElseThrow(() -> new ReservationNotFoundException(phone, dateString));
        reservationRepository.delete(reservation);
    }

    // ✅ Cerca prenotazione per telefono + data
    public ReservationResponse getReservation(String phone, String dateString) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato data non valido. Usa yyyy-MM-dd");
        }
        Reservation reservation = reservationRepository
                .findByUser_PhoneAndDate(phone, date)
                .orElseThrow(() -> new ReservationNotFoundException(phone, dateString));
        return toResponse(reservation);
    }

    // ✅ Trova tutte le prenotazioni per numero
    public List<ReservationResponse> getReservationsByPhone(String phone) {
        return reservationRepository.findAllByUser_Phone(phone).stream()
                .map(this::toResponse)
                .toList();
    }

    // ✅ Trova tutte le prenotazioni per una data specifica
    public List<ReservationResponse> getReservationsByDate(String dateString) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato data non valido. Usa yyyy-MM-dd");
        }
        return reservationRepository.findAllByDate(date).stream()
                .map(this::toResponse)
                .toList();
    }

    // ✅ Orari disponibili per una data
    public List<String> getAvailableTimes(String dateString) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato data non valido. Usa yyyy-MM-dd");
        }
        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) {
            return List.of(); // nessuna prenotazione per date passate
        }

        List<Reservation> reservations = reservationRepository.findAllByDate(date);

        // Orari prenotati
        Map<String, Long> countMap = reservations.stream()
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

            // ⚠️ Se la data è oggi, salta orari già passati
            if (date.equals(today) && start.isBefore(now)) {
                start = start.plusMinutes(30);
                continue;
            }

            long count = countMap.getOrDefault(timeStr, 0L);
            if (count < 12) {
                available.add(timeStr);
            }
            start = start.plusMinutes(30);
        }

        return available;
    }


    // ✅ Solo prenotazioni future per un numero
    public List<ReservationResponse> getFutureReservationsByPhone(String phone) {
        LocalDate today = LocalDate.now();
        return reservationRepository.findAllByUser_Phone(phone).stream()
                .filter(r -> !r.getDate().isBefore(today))
                .map(this::toResponse)
                .toList();
    }

    // ✅ Mapping da entity a response
    private ReservationResponse toResponse(Reservation r) {
        ReservationResponse resp = new ReservationResponse();
        resp.setId(r.getId());
        resp.setName(r.getUser().getName());
        resp.setPhone(r.getUser().getPhone());
        resp.setDate(r.getDate().toString());
        resp.setTime(r.getTime().toString());
        resp.setPeople(r.getPeople());
        resp.setNote(r.getNote());
        return resp;
    }
}
