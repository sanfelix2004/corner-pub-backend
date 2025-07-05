package com.corner.pub.service;

import com.corner.pub.dto.request.ReservationRequest;
import com.corner.pub.dto.response.ReservationResponse;
import com.corner.pub.exception.conflictexception.ReservationAlreadyExistsException;
import com.corner.pub.exception.resourcenotfound.ReservationNotFoundException;
import com.corner.pub.model.Reservation;
import com.corner.pub.model.User;
import com.corner.pub.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserService userService;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository, UserService userService) {
        this.reservationRepository = reservationRepository;
        this.userService = userService;
    }

    /**
     * Crea una nuova prenotazione, creando l'utente se non esiste.
     * Lancia ReservationAlreadyExistsException se esiste già una prenotazione
     * per lo stesso utente e la stessa data.
     */
    public ReservationResponse createReservation(ReservationRequest request) {
        User user = userService.findOrCreate(request.getName(), request.getPhone());

        LocalDate date = LocalDate.parse(request.getDate());
        // verifica prenotazione duplicata
        if (reservationRepository.findByUser_PhoneAndDate(user.getPhone(), date).isPresent()) {
            throw new ReservationAlreadyExistsException(user.getPhone(), request.getDate());
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setDate(date);
        reservation.setTime(LocalTime.parse(request.getTime()));
        reservation.setPeople(request.getPeople());
        reservation.setNote(request.getNote());

        Reservation saved = reservationRepository.save(reservation);

        ReservationResponse response = new ReservationResponse();
        response.setId(saved.getId());
        response.setName(user.getName());
        response.setPhone(user.getPhone());
        response.setDate(saved.getDate().toString());
        response.setTime(saved.getTime().toString());
        response.setPeople(saved.getPeople());
        response.setNote(saved.getNote());

        return response;
    }

    /**
     * Cancella una prenotazione cercando per telefono e data.
     * Lancia ReservationNotFoundException se non trovata.
     */
    public void deleteReservation(String phone, String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        Reservation reservation = reservationRepository
                .findByUser_PhoneAndDate(phone, date)
                .orElseThrow(() -> new ReservationNotFoundException(phone, dateString));
        reservationRepository.delete(reservation);
    }

    public ReservationResponse getReservation(String phone, String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        Reservation reservation = reservationRepository
                .findByUser_PhoneAndDate(phone, date)
                .orElseThrow(() -> new ReservationNotFoundException(phone, dateString));

        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setName(reservation.getUser().getName());
        response.setPhone(reservation.getUser().getPhone());
        response.setDate(reservation.getDate().toString());
        response.setTime(reservation.getTime().toString());
        response.setPeople(reservation.getPeople());
        response.setNote(reservation.getNote());

        return response;
    }

    public List<ReservationResponse> getReservationsByPhone(String phone) {
        List<Reservation> list = reservationRepository.findAllByUser_Phone(phone);
        return list.stream().map(this::toResponse).toList();
    }

    public List<ReservationResponse> getReservationsByDate(String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        List<Reservation> list = reservationRepository.findAllByDate(date);
        return list.stream().map(this::toResponse).toList();
    }

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
    public List<String> getAvailableTimes(String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        List<Reservation> reservations = reservationRepository.findAllByDate(date);

        // Mappa: orario → numero prenotazioni
        Map<String, Long> countMap = reservations.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTime().toString(),
                        Collectors.counting()
                ));

        List<String> available = new ArrayList<>();
        LocalTime start = LocalTime.of(20, 0);
        LocalTime end = LocalTime.of(23, 0);

        while (!start.isAfter(end)) {
            String timeStr = start.toString(); // es: "20:00"
            long count = countMap.getOrDefault(timeStr, 0L);
            if (count < 12) {
                available.add(timeStr);
            }
            start = start.plusMinutes(30);
        }

        return available;
    }

    public List<ReservationResponse> getFutureReservationsByPhone(String phone) {
        LocalDate today = LocalDate.now();
        List<Reservation> list = reservationRepository.findAllByUser_Phone(phone);

        return list.stream()
                .filter(r -> !r.getDate().isBefore(today)) // solo oggi o dopo
                .map(this::toResponse)
                .toList();
    }


}
