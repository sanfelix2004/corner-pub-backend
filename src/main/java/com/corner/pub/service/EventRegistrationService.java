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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.beans.factory.annotation.Value; // aggiunto
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventRegistrationService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final MailService mailService;
    private final String privacyPolicyVersion;

    public EventRegistrationService(UserRepository userRepository,
            EventRepository eventRepository,
            EventRegistrationRepository registrationRepository,
            MailService mailService,
            @Value("${privacy.policy.version}") String privacyPolicyVersion) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.mailService = mailService;
        this.privacyPolicyVersion = privacyPolicyVersion;
    }

    // -----------------------------
    // CREATE
    // -----------------------------
    @Transactional
    public EventRegistrationResponse register(Long eventId, EventRegistrationRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .map(existingUser -> {
                    if (request.getSurname() != null && !request.getSurname().isBlank()) {
                        existingUser.setSurname(request.getSurname());
                    }
                    if (existingUser.getPrivacyPolicyVersion() == null) {
                        existingUser.setPrivacyPolicyVersion(privacyPolicyVersion);
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(request.getName());
                    newUser.setSurname(request.getSurname());
                    newUser.setPhone(request.getPhone());
                    newUser.setPrivacyPolicyVersion(privacyPolicyVersion); // Imposta versione
                    return userRepository.save(newUser);
                });

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CornerPubException("Evento non trovato"));

        // GDPR Strict Compliance
        if (!Boolean.TRUE.equals(request.getPrivacyAccepted())) {
            throw new com.corner.pub.exception.badrequest.PrivacyException(
                    "Per iscriverti all'evento devi accettare il trattamento dei dati personali.");
        }

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

        // Allergeni Guard
        if (request.getAllergensNote() != null && !request.getAllergensNote().trim().isEmpty()) {
            if (!Boolean.TRUE.equals(request.getAllergensConsent())) {
                throw new com.corner.pub.exception.badrequest.AllergenException(
                        "Per inserire allergeni è necessario acconsentire al trattamento di questi dati.");
            }
        }

        EventRegistration registration = new EventRegistration();
        registration.setUser(user);
        registration.setEvent(event);
        registration.setNote(request.getNote());
        registration.setPartecipanti(request.getPartecipanti());
        registration.setPrivacyPolicyVersion(privacyPolicyVersion); // Imposta versione

        if (request.getAllergensNote() != null && !request.getAllergensNote().trim().isEmpty()) {
            registration.setAllergensNote(request.getAllergensNote());
            registration.setAllergensConsent(true);
        }

        EventRegistration saved = registrationRepository.save(registration);
        // notifica via email l'amministratore DOPO il commit della transazione
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mailService.notifyEventRegistrationCreated(saved);
                }
            });
        } else {
            mailService.notifyEventRegistrationCreated(saved);
        }
        return toResponse(saved);
    }

    // -----------------------------
    // UPDATE
    // -----------------------------
    @Transactional
    public EventRegistrationResponse assignTable(Long registrationId, String tableNumber) {
        EventRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registrazione non trovata con id " + registrationId));

        reg.setTableNumber(tableNumber);
        return toResponse(registrationRepository.save(reg));
    }

    // -----------------------------
    // DELETE
    // -----------------------------
    @Transactional
    public void unregister(Long eventId, Long userId) {
        EventRegistration reg = registrationRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new CornerPubException("Registrazione non trovata"));
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mailService.notifyEventRegistrationCancelled(reg);
                }
            });
        } else {
            mailService.notifyEventRegistrationCancelled(reg);
        }
        registrationRepository.delete(reg);
    }

    @Transactional
    public void unregisterByPhone(Long eventId, String phone) {
        EventRegistration reg = registrationRepository
                .findByEventIdAndUser_Phone(eventId, phone)
                .orElseThrow(() -> new CornerPubException("Registrazione non trovata"));
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mailService.notifyEventRegistrationCancelled(reg);
                }
            });
        } else {
            mailService.notifyEventRegistrationCancelled(reg);
        }
        registrationRepository.delete(reg);
    }

    @Transactional
    public void deleteByEventId(Long eventId) {
        registrationRepository.deleteByEventId(eventId);
    }

    // -----------------------------
    // READ
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
    // MAPPING
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
                reg.getPartecipanti());
        resp.setNote(reg.getNote());
        resp.setTableNumber(reg.getTableNumber());
        resp.setAllergensNote(reg.getAllergensNote()); // 🔹 Mappa allergeni
        resp.setPrivacyPolicyVersion(reg.getPrivacyPolicyVersion()); // 🔹 Mappa privacy

        if (userResponse != null) {
            resp.setName(userResponse.getName());
            resp.setSurname(userResponse.getSurname());
            resp.setPhone(userResponse.getPhone());
        }

        return resp;
    }
}
