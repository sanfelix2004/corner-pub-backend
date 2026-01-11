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

    /**
     * Con SendGrid via SMTP lo "username" deve essere la stringa letterale
     * "apikey".
     */
    @Value("${spring.mail.username}")
    private String smtpUser;

    /**
     * Mittente visualizzato: metti un indirizzo reale del tuo dominio (verificato
     * su SendGrid).
     */
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
        if (e == null)
            return false;
        String s = String.valueOf(e.getMessage()).toLowerCase();
        // alcuni provider rispondono con "rate", "too many", "throttl"
        return s.contains("too many") || s.contains("rate") || s.contains("throttl");
    }

    private synchronized void waitForSlot() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastSendAt;
        if (elapsed < minGapMs)
            sleepQuiet(minGapMs - elapsed);
        lastSendAt = System.currentTimeMillis();
    }

    private void sendHtml(String subject, String htmlBody) {
        if (!enabled) {
            log.info("📭 Mail disabilitata (mail.enabled=false). Skippato '{}'", subject);
            return;
        }

        String[] recipients = Arrays.stream(adminTo.split("[,;\\s]+"))
                .filter(s -> s != null && !s.isBlank())
                .toArray(String[]::new);

        waitForSlot();

        int attempts = 0;
        long backoff = 1000;

        while (true) {
            attempts++;
            try {
                jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                        mimeMessage, "utf-8");

                helper.setFrom(from);
                helper.setReplyTo(from);
                helper.setTo(recipients);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);

                mailSender.send(mimeMessage);
                log.info("📧 Email HTML inviata: '{}' -> {}", subject, String.join(", ", recipients));
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

    private String generateWhatsAppLink(String name, String phone, java.time.LocalDate date, java.time.LocalTime time,
            int people) {
        try {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            if (!cleanPhone.startsWith("39")) {
                cleanPhone = "39" + cleanPhone;
            }

            String msg = String.format(
                    "Ciao %s, sono il Corner Pub! Ti confermo la prenotazione per %d persone il giorno %s alle %s. A presto! 🍺",
                    name, people,
                    date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));

            return "https://wa.me/" + cleanPhone + "?text="
                    + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "#";
        }
    }

    private String generateEventWhatsAppLink(String name, String phone, String eventTitle,
            java.time.LocalDateTime eventDate) {
        try {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            if (!cleanPhone.startsWith("39")) {
                cleanPhone = "39" + cleanPhone;
            }

            String msg = String.format(
                    "Ciao %s, sono il Corner Pub! Ti confermo la registrazione all'evento '%s' del %s. A presto! 🍺",
                    name, eventTitle,
                    eventDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            return "https://wa.me/" + cleanPhone + "?text="
                    + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "#";
        }
    }

    public void notifyReservationCreated(Reservation r) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        String waLink = generateWhatsAppLink(r.getUser().getName(), r.getUser().getPhone(), r.getDate(), r.getTime(),
                r.getPeople());

        String body = """
                <html>
                <body style="font-family: sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Nuova prenotazione creata ✅</h2>
                    <ul style="list-style: none; padding: 0;">
                        <li><strong>Nome:</strong> %s</li>
                        <li><strong>Telefono:</strong> %s</li>
                        <li><strong>Data:</strong> %s</li>
                        <li><strong>Ora:</strong> %s</li>
                        <li><strong>Persone:</strong> %d</li>
                        <li><strong>Tavolo:</strong> %s</li>
                        <li><strong>Note:</strong> %s</li>
                        <li><strong>Allergeni:</strong> %s</li>
                        <li><strong>Privacy:</strong> Accettata (%s)</li>
                        <li><strong>ID:</strong> %d</li>
                    </ul>
                    <div style="margin-top: 20px;">
                        <a href="%s" style="background-color: #25D366; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; display: inline-block;">
                           CONFERMA SU WHATSAPP
                        </a>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        safe(r.getUser().getName()),
                        safe(r.getUser().getPhone()),
                        r.getDate().format(df),
                        r.getTime().format(tf),
                        r.getPeople(),
                        r.getTableNumber() == null ? "-" : r.getTableNumber(),
                        r.getNote() == null ? "-" : r.getNote(),
                        r.getAllergensNote() == null ? "-" : r.getAllergensNote(),
                        r.getPrivacyPolicyVersion() == null ? "Sì" : r.getPrivacyPolicyVersion(),
                        r.getId(),
                        waLink);

        sendHtml("Corner • Nuova prenotazione", body);
    }

    public void notifyReservationCancelled(Reservation r) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        String body = """
                <html>
                <body style="font-family: sans-serif; color: #333;">
                    <h2 style="color: #c0392b;">Prenotazione cancellata ❌</h2>
                    <ul>
                        <li><strong>Nome:</strong> %s</li>
                        <li><strong>Data:</strong> %s</li>
                        <li><strong>Ora:</strong> %s</li>
                        <li><strong>ID:</strong> %d</li>
                    </ul>
                </body>
                </html>
                """.formatted(
                safe(r.getUser().getName()),
                r.getDate().format(df),
                r.getTime().format(tf),
                r.getId());

        sendHtml("Corner • Prenotazione cancellata", body);
    }

    public void notifyEventRegistrationCreated(EventRegistration reg) {
        java.time.LocalDateTime dt = reg.getEvent().getData();
        String waLink = generateEventWhatsAppLink(reg.getUser().getName(), reg.getUser().getPhone(),
                reg.getEvent().getTitolo(), dt);

        String body = """
                <html>
                <body style="font-family: sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Nuova iscrizione evento ✅</h2>
                    <ul style="list-style: none; padding: 0;">
                        <li><strong>Evento:</strong> %s</li>
                        <li><strong>Data Evento:</strong> %s</li>
                        <li><strong>Iscritto:</strong> %s (%s)</li>
                        <li><strong>Partecipanti:</strong> %d</li>
                        <li><strong>Note:</strong> %s</li>
                        <li><strong>Allergeni:</strong> %s</li>
                        <li><strong>Privacy:</strong> Accettata (%s)</li>
                        <li><strong>ID:</strong> %d</li>
                    </ul>
                    <div style="margin-top: 20px;">
                        <a href="%s" style="background-color: #25D366; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; display: inline-block;">
                           CONFERMA SU WHATSAPP
                        </a>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        safe(reg.getEvent().getTitolo()),
                        dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        safe(reg.getUser().getName()),
                        safe(reg.getUser().getPhone()),
                        reg.getPartecipanti(),
                        reg.getNote() == null ? "-" : reg.getNote(),
                        reg.getAllergensNote() == null ? "-" : reg.getAllergensNote(),
                        reg.getPrivacyPolicyVersion() == null ? "Sì" : reg.getPrivacyPolicyVersion(),
                        reg.getId(),
                        waLink);

        sendHtml("Corner • Nuova iscrizione evento", body);
    }

    public void notifyEventRegistrationCancelled(EventRegistration reg) {
        String body = """
                <html>
                <body style="font-family: sans-serif; color: #333;">
                    <h2 style="color: #c0392b;">Iscrizione evento cancellata ❌</h2>
                    <ul>
                        <li><strong>Evento:</strong> %s</li>
                        <li><strong>Utente:</strong> %s</li>
                        <li><strong>ID:</strong> %d</li>
                    </ul>
                </body>
                </html>
                """.formatted(
                safe(reg.getEvent().getTitolo()),
                safe(reg.getUser().getName()),
                reg.getId());

        sendHtml("Corner • Iscrizione evento cancellata", body);
    }

    // -------------------------------------------------------------------

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}