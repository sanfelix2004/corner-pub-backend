package com.corner.pub.service;

import com.corner.pub.dto.request.EventRequest;
import com.corner.pub.dto.request.RunClubEventRequest;
import com.corner.pub.dto.response.EventRegistrationResponse;
import com.corner.pub.dto.response.EventResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.model.Event;
import com.corner.pub.model.EventRegistration;
import com.corner.pub.repository.EventRegistrationRepository;
import com.corner.pub.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RunClubService {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final Pattern TICKET = Pattern.compile("CRC-[A-Z0-9]{6,12}");

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventService eventService;
    private final EventRegistrationService registrationService;
    private final RunClubTicketService ticketService;

    public RunClubService(EventRepository eventRepository,
                          EventRegistrationRepository registrationRepository,
                          EventService eventService,
                          EventRegistrationService registrationService,
                          RunClubTicketService ticketService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.ticketService = ticketService;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listPublic() {
        return eventRepository.findByRunClubTrueAndDataAfterOrderByDataAsc(LocalDateTime.now(ROME)).stream()
                .filter(event -> !event.isAnnullata())
                .filter(this::isOccurrence)
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listAdmin() {
        return eventRepository.findByRunClubTrueOrderByDataAsc().stream()
                .filter(this::isOccurrence)
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventResponse create(RunClubEventRequest request) {
        validate(request, true);
        EventRequest eventRequest = new EventRequest();
        eventRequest.setTitolo(request.getTitolo().trim());
        eventRequest.setDescrizione(blankToNull(request.getDescrizione()));
        eventRequest.setData(request.getData());
        eventRequest.setPostiTotali(request.getPostiTotali());
        eventRequest.setLuogo(request.getLuogo().trim());
        eventRequest.setDistanza(request.getDistanza().trim());
        eventRequest.setRitmo(blankToNull(request.getRitmo()));
        eventRequest.setQuotaEuro(request.getQuotaEuro() == null ? 0 : request.getQuotaEuro());
        eventRequest.setRunClub(true);
        eventRequest.setSpeciale(false);
        eventRequest.setRicorrente(Boolean.TRUE.equals(request.getRicorrente()));
        eventRequest.setGiornoSettimana(request.getGiornoSettimana());
        eventRequest.setDataFine(request.getDataFine());
        return eventService.createEvent(eventRequest, null);
    }

    @Transactional
    public EventResponse update(Long id, RunClubEventRequest request) {
        validate(request, false);
        Event event = requireRun(id);
        event.setTitolo(request.getTitolo().trim());
        event.setDescrizione(blankToNull(request.getDescrizione()));
        event.setData(request.getData());
        event.setPostiTotali(request.getPostiTotali());
        event.setLuogo(request.getLuogo().trim());
        event.setDistanza(request.getDistanza().trim());
        event.setRitmo(blankToNull(request.getRitmo()));
        event.setQuotaEuro(request.getQuotaEuro() == null ? 0 : request.getQuotaEuro());
        eventRepository.save(event);

        if (Boolean.TRUE.equals(request.getApplicaATuttaLaSerie())) {
            Event root = event.getParentEvent() != null ? event.getParentEvent() : event;
            applySeriesText(root, event);
            for (Event child : eventRepository.findByParentEventIdOrderByDataAsc(root.getId())) {
                applySeriesText(child, event);
                eventRepository.save(child);
            }
            eventRepository.save(root);
        }
        return map(event);
    }

    @Transactional
    public EventResponse setAnnullata(Long id, boolean annullata) {
        Event event = requireRun(id);
        event.setAnnullata(annullata);
        return map(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long id) {
        requireRun(id);
        long iscritti = registrationRepository.countByEventId(id);
        if (iscritti > 0) {
            throw new CornerPubException(
                    "Ci sono iscritti. Annulla l'uscita oppure rimuovi prima le iscrizioni.");
        }
        eventService.deleteEvent(id);
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> attendees(Long eventId) {
        requireRun(eventId);
        return registrationService.getRegistrationsByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> recover(Long eventId, String phone) {
        Event event = requireRun(eventId);
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        EventRegistration registration = registrationRepository.findByEventIdAndUser_Phone(event.getId(), digits)
                .orElseThrow(() -> new CornerPubException(
                        "Nessuna iscrizione trovata con questo numero per l'uscita scelta."));
        if (registration.getTicketCode() == null || registration.getTicketCode().isBlank()) {
            throw new CornerPubException("Il pass non è ancora disponibile. Scrivi al Corner.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ticketCode", registration.getTicketCode());
        body.put("name", registration.getUser().getName());
        body.put("surname", registration.getUser().getSurname());
        body.put("eventId", event.getId());
        body.put("eventTitle", event.getTitolo());
        return body;
    }

    @Transactional(readOnly = true)
    public byte[] pdf(String rawCode) {
        EventRegistration registration = requireTicket(rawCode);
        registration.getUser().getName();
        registration.getUser().getSurname();
        registration.getEvent().getTitolo();
        registration.getEvent().getData();
        registration.getEvent().getLuogo();
        registration.getEvent().isAnnullata();
        registration.getEvent().getQuotaEuro();
        registration.getPaceCategory();
        registration.getPaymentStatus();
        return ticketService.render(registration);
    }

    @Transactional
    public Map<String, Object> checkIn(String rawCode, String action) {
        EventRegistration registration = requireTicket(rawCode);
        String mode = action == null || action.isBlank() ? "presente-incassa" : action.trim();
        if (!java.util.List.of("presente", "incassa", "presente-incassa").contains(mode)) {
            throw new CornerPubException("Azione di check-in non valida.");
        }
        boolean alreadyPresent = registration.getCheckedInAt() != null;
        boolean alreadyPaid = "PAID".equals(registration.getPaymentStatus()) || "FREE".equals(registration.getPaymentStatus());
        int quota = registration.getEvent().getQuotaEuro() == null ? 0 : registration.getEvent().getQuotaEuro();
        if ("incassa".equals(mode) || "presente-incassa".equals(mode)) {
            if (quota > 0 && !"PAID".equals(registration.getPaymentStatus())) {
                registration.setPaymentStatus("PAID");
                registration.setPaidAt(LocalDateTime.now(ROME));
            }
        }
        if (!"incassa".equals(mode) && !alreadyPresent) {
            registration.setCheckedInAt(LocalDateTime.now(ROME));
        }
        registrationRepository.save(registration);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("alreadyCheckedIn", alreadyPresent && !"incassa".equals(mode));
        body.put("alreadyPaid", alreadyPaid);
        body.put("registration", registrationService.toResponse(registration));
        return body;
    }

    @Transactional
    public Map<String, Object> undoCheckIn(String rawCode) {
        EventRegistration registration = requireTicket(rawCode);
        registration.setCheckedInAt(null);
        registrationRepository.save(registration);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("alreadyCheckedIn", false);
        body.put("registration", registrationService.toResponse(registration));
        return body;
    }

    @Transactional
    public Map<String, Object> undoPayment(String rawCode) {
        EventRegistration registration = requireTicket(rawCode);
        int quota = registration.getEvent().getQuotaEuro() == null ? 0 : registration.getEvent().getQuotaEuro();
        registration.setPaidAt(null);
        registration.setPaymentStatus(quota > 0 ? "PENDING_PAYMENT" : "FREE");
        registrationRepository.save(registration);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("registration", registrationService.toResponse(registration));
        return body;
    }

    private EventRegistration requireTicket(String rawCode) {
        String code = normalizeCode(rawCode);
        return registrationRepository.findByTicketCode(code)
                .orElseThrow(() -> new CornerPubException("Pass non trovato. Controlla il codice."));
    }

    private Event requireRun(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new CornerPubException("Uscita non trovata"));
        if (!event.isRunClub()) {
            throw new CornerPubException("Uscita non trovata");
        }
        return event;
    }

    private boolean isOccurrence(Event event) {
        return !(event.isRicorrente() && event.getParentEvent() == null);
    }

    private EventResponse map(Event event) {
        long totale = registrationRepository.sumPartecipantiByEventId(event.getId()).orElse(0L);
        EventResponse response = new EventResponse(event, totale);
        response.setFigliCount((int) eventRepository.countByParentEventId(event.getId()));
        return response;
    }

    private void applySeriesText(Event target, Event source) {
        target.setTitolo(source.getTitolo());
        target.setDescrizione(source.getDescrizione());
        target.setPostiTotali(source.getPostiTotali());
        target.setLuogo(source.getLuogo());
        target.setDistanza(source.getDistanza());
        target.setRitmo(source.getRitmo());
        target.setQuotaEuro(source.getQuotaEuro());
        target.setRunClub(true);
    }

    private void validate(RunClubEventRequest request, boolean creating) {
        if (request == null) {
            throw new CornerPubException("Dati mancanti.");
        }
        if (request.getTitolo() == null || request.getTitolo().isBlank()) {
            throw new CornerPubException("Il titolo è obbligatorio.");
        }
        if (request.getTitolo().trim().length() > 100) {
            throw new CornerPubException("Il titolo non può superare i 100 caratteri.");
        }
        if (request.getData() == null) {
            throw new CornerPubException("Data e ora di ritrovo sono obbligatorie.");
        }
        if (creating && !request.getData().isAfter(LocalDateTime.now(ROME))) {
            throw new CornerPubException("La data di ritrovo deve essere nel futuro.");
        }
        if (request.getPostiTotali() == null || request.getPostiTotali() < 1) {
            throw new CornerPubException("Indica i posti disponibili, almeno 1.");
        }
        if (request.getLuogo() == null || request.getLuogo().isBlank()) {
            throw new CornerPubException("Il ritrovo è obbligatorio.");
        }
        if (request.getLuogo().trim().length() > 180) {
            throw new CornerPubException("Il ritrovo è troppo lungo.");
        }
        if (request.getDistanza() == null || request.getDistanza().isBlank()) {
            throw new CornerPubException("La distanza è obbligatoria.");
        }
        if (request.getDistanza().trim().length() > 40) {
            throw new CornerPubException("La distanza è troppo lunga.");
        }
        if (request.getRitmo() != null && request.getRitmo().trim().length() > 40) {
            throw new CornerPubException("Il ritmo è troppo lungo.");
        }
        if (request.getQuotaEuro() != null && request.getQuotaEuro() < 0) {
            throw new CornerPubException("La quota non può essere negativa.");
        }
        if (request.getDescrizione() != null && request.getDescrizione().trim().length() > 240) {
            throw new CornerPubException("La descrizione può avere al massimo 240 caratteri.");
        }
        if (Boolean.TRUE.equals(request.getRicorrente())) {
            Integer day = request.getGiornoSettimana();
            if (day == null || day < 1 || day > 7) {
                throw new CornerPubException("Seleziona il giorno della settimana.");
            }
        }
    }

    private String normalizeCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new CornerPubException("Inserisci il codice del pass.");
        }
        Matcher matcher = TICKET.matcher(raw.trim().toUpperCase(Locale.ROOT));
        if (!matcher.find()) {
            throw new CornerPubException("Codice pass non valido.");
        }
        return matcher.group();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
