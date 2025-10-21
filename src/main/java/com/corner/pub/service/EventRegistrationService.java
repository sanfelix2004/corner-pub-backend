package com.corner.pub.service;

import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.response.EventRegistrationResponse;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.dto.response.UserResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.exception.resourcenotfound.ResourceNotFoundException;
import com.corner.pub.model.Event;
import com.corner.pub.model.EventRegistration;
import com.corner.pub.model.User;
import com.corner.pub.repository.EventRegistrationRepository;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventRegistrationService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;

    public EventRegistrationService(UserRepository userRepository,
                                    EventRepository eventRepository,
                                    EventRegistrationRepository registrationRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    // -----------------------------
    //           CREATE
    // -----------------------------
    @Transactional
    public EventRegistrationResponse register(Long eventId, EventRegistrationRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(request.getName());
                    newUser.setPhone(request.getPhone());
                    return userRepository.save(newUser);
                });

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        // 🔒 controllo: stesso giorno
        LocalDate eventDate = event.getData().toLocalDate();
        if (hasEventSameDay(user.getPhone(), eventDate)) {
            throw new CornerPubException("Non puoi registrarti: sei già iscritto a un evento nello stesso giorno.");
        }

        if (registrationRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            throw new CornerPubException("Sei già iscritto a questo evento");
        }

        if (event.getPostiTotali() != null) {
            long iscritti = getTotalePartecipantiByEventId(eventId);
            if (iscritti + request.getPartecipanti() > event.getPostiTotali()) {
                throw new CornerPubException("Posti esauriti per questo evento");
            }
        }

        EventRegistration registration = new EventRegistration();
        registration.setUser(user);
        registration.setEvent(event);
        registration.setNote(request.getNote());
        registration.setPartecipanti(request.getPartecipanti());

        return toResponse(registrationRepository.save(registration));
    }

    // -----------------------------
    //           UPDATE
    // -----------------------------
    @Transactional
    public EventRegistrationResponse assignTable(Long registrationId, String tableNumber) {
        EventRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registrazione non trovata con id " + registrationId));

        reg.setTableNumber(tableNumber);
        return toResponse(registrationRepository.save(reg));
    }

    // -----------------------------
    //           DELETE
    // -----------------------------
    @Transactional
    public void unregister(Long eventId, Long userId) {
        EventRegistration reg = registrationRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new CornerPubException("Registrazione non trovata"));
        registrationRepository.delete(reg);
    }

    @Transactional
    public void unregisterByPhone(Long eventId, String phone) {
        EventRegistration reg = registrationRepository
                .findByEventIdAndUser_Phone(eventId, phone)
                .orElseThrow(() -> new CornerPubException("Registrazione non trovata"));
        registrationRepository.delete(reg);
    }

    @Transactional
    public void deleteByEventId(Long eventId) {
        registrationRepository.deleteByEventId(eventId);
    }

    // -----------------------------
    //           READ
    // -----------------------------
    @Transactional(readOnly = true)
    public long getTotalePartecipantiByEventId(Long eventId) {
        return registrationRepository.findByEventId(eventId).stream()
                .mapToLong(EventRegistration::getPartecipanti)
                .sum();
    }

    @Transactional(readOnly = true)
    public long countByEventId(Long eventId) {
        return registrationRepository.countByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> getRegistrationsByEventId(Long eventId) {
        List<EventRegistration> registrations = registrationRepository.findByEventId(eventId);
        long totaleIscritti = registrations.stream().mapToLong(EventRegistration::getPartecipanti).sum();
        return registrations.stream()
                .map(r -> toResponse(r, totaleIscritti))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> getRegistrationsByPhone(String phone) {
        List<EventRegistration> regs = registrationRepository.findByUser_Phone(phone);
        long totaleIscritti = regs.stream().mapToLong(EventRegistration::getPartecipanti).sum();
        return regs.stream()
                .map(r -> toResponse(r, totaleIscritti))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> getAllRegistrations() {
        return registrationRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean hasEventSameDay(String phone, LocalDate date) {
        return registrationRepository.existsByPhoneAndEventDate(phone, date);
    }

    // -----------------------------
    //        MAPPING
    // -----------------------------
    public EventRegistrationResponse toResponse(EventRegistration reg) {
        long totalePartecipanti = registrationRepository.findByEventId(reg.getEvent().getId())
                .stream()
                .mapToLong(EventRegistration::getPartecipanti)
                .sum();
        return toResponse(reg, totalePartecipanti);
    }

    private EventRegistrationResponse toResponse(EventRegistration reg, long totalePartecipanti) {
        EventResponse eventResponse = reg.getEvent() != null
                ? new EventResponse(reg.getEvent(), totalePartecipanti)
                : null;

        UserResponse userResponse = reg.getUser() != null
                ? new UserResponse(reg.getUser())
                : null;

        EventRegistrationResponse resp = new EventRegistrationResponse(
                reg.getId(),
                reg.getCreatedAt(),
                eventResponse,
                userResponse,
                reg.getPartecipanti()
        );
        resp.setNote(reg.getNote());
        resp.setTableNumber(reg.getTableNumber());

        if (userResponse != null) {
            resp.setName(userResponse.getName());
            resp.setPhone(userResponse.getPhone());
        }

        return resp;
    }
}
