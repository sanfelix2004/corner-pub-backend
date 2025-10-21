package com.corner.pub.service;

import com.corner.pub.model.EventRegistration;
import com.corner.pub.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    // --- SMTP / indirizzi ----------------------------------------------

    /** Con SendGrid via SMTP lo "username" deve essere la stringa letterale "apikey". */
    @Value("${spring.mail.username}")
    private String smtpUser;

    /** Mittente visualizzato: metti un indirizzo reale del tuo dominio (verificato su SendGrid). */
    @Value("${mail.from.noreply:noreply@corner.pub}")
    private String from;

    /** Uno o più destinatari separati da virgola/; o spazi. */
    @Value("${mail.to.admin:cornersnc@gmail.com}")
    private String adminTo;

    /** Abilita/disabilita totalmente l’invio. */
    @Value("${mail.enabled:true}")
    private boolean enabled;

    /** Minimo intervallo tra due invii (ms). */
    @Value("${mail.rate-ms:800}")
    private long minGapMs;

    // -------------------------------------------------------------------

    private volatile long lastSendAt = 0L;

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean looksLikeRateLimit(Throwable e) {
        if (e == null) return false;
        String s = String.valueOf(e.getMessage()).toLowerCase();
        // alcuni provider rispondono con "rate", "too many", "throttl"
        return s.contains("too many") || s.contains("rate") || s.contains("throttl");
    }

    private synchronized void waitForSlot() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastSendAt;
        if (elapsed < minGapMs) sleepQuiet(minGapMs - elapsed);
        lastSendAt = System.currentTimeMillis();
    }

    private void send(String subject, String body) {
        if (!enabled) {
            log.info("📭 Mail disabilitata (mail.enabled=false). Skippato '{}'", subject);
            return;
        }

        // normalizza lista destinatari
        String[] recipients = Arrays.stream(adminTo.split("[,;\\s]+"))
                .filter(s -> s != null && !s.isBlank())
                .toArray(String[]::new);

        // piccolo throttle locale
        waitForSlot();

        int attempts = 0;
        long backoff = 1000; // 1s -> 2s -> 4s

        while (true) {
            attempts++;
            try {
                SimpleMailMessage msg = new SimpleMailMessage();

                /*
                 * Con SendGrid: l’SMTP username è letteralmente "apikey" e il mittente
                 * DEVE essere un indirizzo autorizzato/verified in SendGrid (from).
                 */
                msg.setFrom(from);

                // Imposto Reply-To al mittente “logico” (facoltativo)
                msg.setReplyTo(from);

                msg.setTo(recipients);
                msg.setSubject(subject);
                msg.setText(body);

                mailSender.send(msg);
                log.info("📧 Email inviata: '{}' -> {}", subject, String.join(", ", recipients));
                return;

            } catch (Exception e) {
                if (looksLikeRateLimit(e) && attempts < 3) {
                    log.warn("⏳ Probabile rate-limit SMTP, retry #{} tra {} ms", attempts, backoff);
                    sleepQuiet(backoff);
                    backoff *= 2;
                } else {
                    log.error("❌ Invio email fallito: {}", e.getMessage(), e);
                    return;
                }
            }
        }
    }

    // ================== Notifiche dominio Piattaforma ===================

    // ---------- Prenotazioni ----------
    public void notifyReservationCreated(Reservation r) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        String body = """
                Nuova prenotazione creata ✅

                Nome: %s
                Telefono: %s
                Data: %s
                Ora: %s
                Persone: %d
                Tavolo: %s
                Note: %s
                ID prenotazione: %d
                """.formatted(
                safe(r.getUser().getName()),
                safe(r.getUser().getPhone()),
                r.getDate().format(df),
                r.getTime().format(tf),
                r.getPeople(),
                r.getTableNumber() == null ? "-" : r.getTableNumber(),
                r.getNote() == null ? "-" : r.getNote(),
                r.getId()
        );

        send("Corner • Nuova prenotazione", body);
    }

    public void notifyReservationCancelled(Reservation r) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        String body = """
                Prenotazione cancellata ❌

                Nome: %s
                Telefono: %s
                Data: %s
                Ora: %s
                Persone: %d
                Tavolo: %s
                Note: %s
                ID prenotazione: %d
                """.formatted(
                safe(r.getUser().getName()),
                safe(r.getUser().getPhone()),
                r.getDate().format(df),
                r.getTime().format(tf),
                r.getPeople(),
                r.getTableNumber() == null ? "-" : r.getTableNumber(),
                r.getNote() == null ? "-" : r.getNote(),
                r.getId()
        );

        send("Corner • Prenotazione cancellata", body);
    }

    // ---------- Eventi ----------
    public void notifyEventRegistrationCreated(EventRegistration reg) {
        String body = """
                Nuova iscrizione evento ✅

                Evento: %s
                Data/Ora: %s
                Iscritto: %s (%s)
                Partecipanti: %d
                Note: %s
                ID registrazione: %d
                """.formatted(
                safe(reg.getEvent().getTitolo()),
                String.valueOf(reg.getEvent().getData()),
                safe(reg.getUser().getName()),
                safe(reg.getUser().getPhone()),
                reg.getPartecipanti(),
                reg.getNote() == null ? "-" : reg.getNote(),
                reg.getId()
        );

        send("Corner • Nuova iscrizione evento", body);
    }

    public void notifyEventRegistrationCancelled(EventRegistration reg) {
        String body = """
                Iscrizione evento cancellata ❌

                Evento: %s
                Data/Ora: %s
                Utente: %s (%s)
                Partecipanti: %d
                ID registrazione: %d
                """.formatted(
                safe(reg.getEvent().getTitolo()),
                String.valueOf(reg.getEvent().getData()),
                safe(reg.getUser().getName()),
                safe(reg.getUser().getPhone()),
                reg.getPartecipanti(),
                reg.getId()
        );

        send("Corner • Iscrizione evento cancellata", body);
    }

    // -------------------------------------------------------------------

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}