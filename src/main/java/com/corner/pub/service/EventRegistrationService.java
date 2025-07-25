package com.corner.pub.service;

import com.corner.pub.dto.request.EventRegistrationRequest;
import com.corner.pub.dto.response.EventRegistrationResponse;
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

    public void register(Long eventId, EventRegistrationRequest request) {
        // Trova o crea l'utente
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(request.getName());
                    newUser.setPhone(request.getPhone());
                    return userRepository.save(newUser);
                });

        // Trova l'evento
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        // Controlla se l'utente è già iscritto
        if (registrationRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            throw new CornerPubException("Sei già iscritto a questo evento");
        }

        // Se ci sono limiti, controlla disponibilità
        if (event.getPostiTotali() != null) {
            long iscritti = registrationRepository.countByEventId(eventId);
            if (iscritti >= event.getPostiTotali()) {
                throw new CornerPubException("Posti esauriti per questo evento");
            }
        }

        // Registra l'utente all'evento
        EventRegistration registration = new EventRegistration();
        registration.setUser(user);
        registration.setEvent(event);
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
        return registrationRepository.findByEventId(eventId).stream()
                .map(reg -> new EventRegistrationResponse(
                        reg.getUser().getName(),
                        reg.getUser().getPhone(),
                        reg.getCreatedAt() // deve esistere nella entity
                ))
                .collect(Collectors.toList());
    }

}