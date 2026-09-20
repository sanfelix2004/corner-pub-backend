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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventRegistrationService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final MailService mailService;
    private final String privacyPolicyVersion;
    private final String publicBaseUrl;

    public EventRegistrationService(UserRepository userRepository,
            EventRepository eventRepository,
            EventRegistrationRepository registrationRepository,
            MailService mailService,
            @Value("${privacy.policy.version}") String privacyPolicyVersion,
            @Value("${app.public-base-url:https://cornerpubgiovinazzo.com}") String publicBaseUrl) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.mailService = mailService;
        this.privacyPolicyVersion = privacyPolicyVersion;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
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
        if (saved.getUser() != null) {
            saved.getUser().getName();
            saved.getUser().getSurname();
            saved.getUser().getPhone();
        }
        if (saved.getEvent() != null) {
            saved.getEvent().getTitolo();
            saved.getEvent().getData();
        }
        // notifica via email l'amministratore DOPO il commit della transazione
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        mailService.notifyEventRegistrationCreated(saved);
                    } catch (Exception ignored) {
                    }
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
                    try {
                        mailService.notifyEventRegistrationCancelled(reg);
                    } catch (Exception ignored) {
                    }
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
                    try {
                        mailService.notifyEventRegistrationCancelled(reg);
                    } catch (Exception ignored) {
                    }
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
        resp.setAttending(reg.getAttending());
        resp.setAttendanceRespondedAt(reg.getAttendanceRespondedAt());

        if (userResponse != null) {
            resp.setName(userResponse.getName());
            resp.setSurname(userResponse.getSurname());
            resp.setPhone(userResponse.getPhone());
        }

        return resp;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAttendanceByToken(String token) {
        EventRegistration reg = registrationRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Link di conferma non valido"));
        return attendancePayload(reg);
    }

    @Transactional
    public Map<String, Object> saveAttendance(String token, boolean attending) {
        EventRegistration reg = registrationRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Link di conferma non valido"));
        reg.setAttending(attending);
        reg.setAttendanceRespondedAt(LocalDateTime.now());
        registrationRepository.save(reg);
        return attendancePayload(reg);
    }

    @Transactional
    public Map<String, Object> prepareAttendanceReminders(Long eventId, String testPhone) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento non trovato"));
        List<EventRegistration> regs = registrationRepository.findByEventId(eventId);
        String wanted = normalizePhone(testPhone);
        if (wanted != null) {
            regs = regs.stream()
                    .filter(reg -> wanted.equals(normalizePhone(reg.getUser() != null ? reg.getUser().getPhone() : null)))
                    .collect(Collectors.toList());
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        for (EventRegistration reg : regs) {
            if (reg.getConfirmationToken() == null || reg.getConfirmationToken().isBlank()) {
                reg.setConfirmationToken(UUID.randomUUID().toString().replace("-", ""));
                registrationRepository.save(reg);
            }
            String phone = reg.getUser() != null ? reg.getUser().getPhone() : "";
            String name = reg.getUser() != null ? safe(reg.getUser().getName()) : "ciao";
            String confirmUrl = publicBaseUrl + "/conferma-evento.html?t=" + reg.getConfirmationToken();
            String text = buildWhatsAppText(name, event, confirmUrl);
            messages.add(Map.of(
                    "registrationId", reg.getId(),
                    "name", name,
                    "phone", phone,
                    "waLink", whatsAppLink(phone, text),
                    "confirmUrl", confirmUrl
            ));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", event.getId());
        result.put("eventTitle", event.getTitolo());
        result.put("targeted", messages.size());
        result.put("testOnly", wanted != null);
        result.put("messages", messages);
        return result;
    }

    private Map<String, Object> attendancePayload(EventRegistration reg) {
        Event event = reg.getEvent();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", reg.getUser() != null ? safe(reg.getUser().getName()) : "");
        payload.put("eventTitle", event != null ? event.getTitolo() : "Evento");
        payload.put("eventDate", event != null && event.getData() != null
                ? event.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "");
        payload.put("attending", reg.getAttending());
        return payload;
    }

    private String buildWhatsAppText(String name, Event event, String confirmUrl) {
        String when = event.getData() != null
                ? event.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "11/09";
        return "Ciao " + name + ", siamo il Corner Pub.\n\n"
                + "Confermi la partecipazione all'evento \"" + event.getTitolo() + "\" del " + when + "?\n\n"
                + "Apri questo link e scegli Sì o No:\n"
                + confirmUrl + "\n\n"
                + "Grazie!";
    }

    private String whatsAppLink(String phone, String text) {
        String digits = digitsOnly(phone);
        if (digits.isEmpty()) {
            return "#";
        }
        if (!digits.startsWith("39")) {
            digits = "39" + digits;
        }
        return "https://wa.me/" + digits + "?text="
                + java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String normalizePhone(String phone) {
        String digits = digitsOnly(phone);
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.startsWith("39") && digits.length() > 10) {
            digits = digits.substring(2);
        }
        return digits;
    }

    private String digitsOnly(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
