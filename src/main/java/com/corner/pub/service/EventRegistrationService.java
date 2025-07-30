package com.corner.pub.service;

import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.response.EventRegistrationResponse;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.dto.response.UserResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.model.Event;
import com.corner.pub.model.EventRegistration;
import com.corner.pub.model.User;
import com.corner.pub.repository.EventRegistrationRepository;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 🔑 nuovo metodo
    @Transactional(readOnly = true)
    public long getTotalePartecipantiByEventId(Long eventId) {
        List<EventRegistration> regs = registrationRepository.findByEventId(eventId);
        long totale = regs.stream()
                .mapToLong(EventRegistration::getPartecipanti)
                .sum();
        return totale;
    }



    public void register(Long eventId, EventRegistrationRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(request.getName());
                    newUser.setPhone(request.getPhone());
                    return userRepository.save(newUser);
                });

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

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
        registrationRepository.save(registration);
    }


    @Transactional(readOnly = true)
    public long countByEventId(Long eventId) {
        return registrationRepository.countByEventId(eventId);
    }

    @Transactional
    public void deleteByEventId(Long eventId) {
        registrationRepository.deleteByEventId(eventId);
    }

    public List<EventRegistrationResponse> getRegistrationsByEventId(Long eventId) {
        List<EventRegistration> registrations = registrationRepository.findByEventId(eventId);

        long totaleIscritti = registrations.stream()
                .mapToLong(EventRegistration::getPartecipanti)
                .sum();

        return registrations.stream()
                .map(reg -> {
                    EventResponse eventResponse = new EventResponse(reg.getEvent(), totaleIscritti);
                    UserResponse userResponse = new UserResponse(reg.getUser());

                    EventRegistrationResponse resp = new EventRegistrationResponse(
                            reg.getId(),
                            reg.getCreatedAt(),
                            eventResponse,
                            userResponse,
                            reg.getPartecipanti()
                    );
                    resp.setNote(reg.getNote());
                    resp.setName(userResponse.getName());
                    resp.setPhone(userResponse.getPhone());

                    return resp;
                })
                .collect(Collectors.toList());
    }


    @Transactional
    public void unregister(Long eventId, Long userId) {
        EventRegistration reg = registrationRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new CornerPubException("Registrazione non trovata"));

        registrationRepository.delete(reg);
    }

    public List<EventRegistrationResponse> getAllRegistrations() {
        return registrationRepository.findAll().stream()
                .map(reg -> {
                    long totalePartecipanti = registrationRepository.findByEventId(reg.getEvent().getId())
                            .stream()
                            .mapToLong(EventRegistration::getPartecipanti)
                            .sum();

                    EventResponse eventResponse = null;
                    if (reg.getEvent() != null) {
                        eventResponse = new EventResponse(reg.getEvent(), totalePartecipanti);
                    }

                    UserResponse userResponse = null;
                    if (reg.getUser() != null) {
                        userResponse = new UserResponse(reg.getUser());
                    }

                    EventRegistrationResponse resp = new EventRegistrationResponse(
                            reg.getId(),
                            reg.getCreatedAt(),
                            eventResponse,
                            userResponse,
                            reg.getPartecipanti()
                    );
                    resp.setNote(reg.getNote());

                    if (userResponse != null) {
                        resp.setName(userResponse.getName());
                        resp.setPhone(userResponse.getPhone());
                    }

                    return resp;
                })
                .collect(Collectors.toList());
    }

    public long sumPartecipantiByEventId(Long eventId) {
        return registrationRepository.sumPartecipantiByEventId(eventId).orElse(0L);
    }

}
