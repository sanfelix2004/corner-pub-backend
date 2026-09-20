package com.corner.pub.service;

import com.corner.pub.model.EventRegistration;
import com.corner.pub.model.Reservation;
import com.corner.pub.model.User;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class MailService {

    private static final String FALLBACK_ADMIN = "sanfelicefrancesco004@gmail.com";
    private static final String FALLBACK_FROM = "sanfelicefrancesco004@gmail.com";

    private final JavaMailSender mailSender;
    private final ExecutorService mailPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mail-send");
        t.setDaemon(true);
        return t;
    });

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${mail.from.noreply:sanfelicefrancesco004@gmail.com}")
    private String from;

    @Value("${mail.to.admin:sanfelicefrancesco004@gmail.com}")
    private String adminTo;

    @Value("${mail.enabled:true}")
    private boolean enabled;

    @Value("${mail.rate-ms:800}")
    private long minGapMs;

    private volatile long lastSendAt = 0L;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    void logMailConfig() {
        log.info("📬 Mail attiva={} host={} from={} to={}", enabled, smtpHost, resolveFrom(),
                String.join(",", resolveRecipients()));
    }

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
        return s.contains("too many") || s.contains("rate") || s.contains("throttl");
    }

    private synchronized void waitForSlot() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastSendAt;
        if (elapsed < minGapMs)
            sleepQuiet(minGapMs - elapsed);
        lastSendAt = System.currentTimeMillis();
    }

    private String resolveFrom() {
        if (from != null && from.contains("@"))
            return from.trim();
        return FALLBACK_FROM;
    }

    private String[] resolveRecipients() {
        String raw = (adminTo == null || adminTo.isBlank()) ? FALLBACK_ADMIN : adminTo;
        String[] recipients = Arrays.stream(raw.split("[,;\\s]+"))
                .filter(s -> s != null && s.contains("@"))
                .toArray(String[]::new);
        if (recipients.length == 0) {
            return new String[] { FALLBACK_ADMIN };
        }
        return recipients;
    }

    private void sendHtml(String subject, String htmlBody) {
        if (!enabled) {
            log.info("📭 Mail disabilitata (mail.enabled=false). Skippato '{}'", subject);
            return;
        }
        String html = htmlBody;
        mailPool.execute(() -> doSend(subject, html));
    }

    private void doSend(String subject, String htmlBody) {
        String[] recipients = resolveRecipients();
        waitForSlot();

        int attempts = 0;
        long backoff = 1000;

        while (true) {
            attempts++;
            try {
                jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
                helper.setFrom(new InternetAddress(resolveFrom(), "Corner Pub", "UTF-8"));
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

    private String whatsAppLink(String phone, String message) {
        try {
            String cleanPhone = phone == null ? "" : phone.replaceAll("[^0-9]", "");
            if (cleanPhone.isBlank() || "-".equals(cleanPhone)) {
                return "#";
            }
            if (!cleanPhone.startsWith("39")) {
                cleanPhone = "39" + cleanPhone;
            }
            return "https://wa.me/" + cleanPhone + "?text="
                    + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "#";
        }
    }

    public void notifyReservationCreated(Reservation r) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        User user = r.getUser();
        String firstName = user != null ? safe(user.getName()) : "-";
        String fullName = fullName(user);
        String phone = user != null ? safe(user.getPhone()) : "-";
        String date = r.getDate().format(df);
        String time = r.getTime().format(tf);
        String waMsg = "Ciao " + firstName
                + ", siamo il Corner Pub. Ti confermiamo il tavolo per " + r.getPeople()
                + " persone il " + date + " alle " + time
                + ". Ti aspettiamo in Piazza Duomo 58, Giovinazzo. A presto!";

        String rows = row("Cliente", fullName)
                + row("Telefono", phone)
                + row("Giorno", date)
                + row("Ora", time)
                + row("Persone", String.valueOf(r.getPeople()))
                + row("Tavolo", r.getTableNumber())
                + row("Note", r.getNote())
                + row("Allergeni", r.getAllergensNote());

        sendHtml("Nuova prenotazione · " + fullName + " · " + date + " " + time,
                staffEmail("Nuova prenotazione tavolo",
                        "È arrivata una prenotazione dal sito o dal back office.",
                        rows, whatsAppLink(phone, waMsg),
                        "Apri WhatsApp e conferma al cliente"));
    }

    public void notifyReservationCancelled(Reservation r) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        String fullName = fullName(r.getUser());
        sendHtml("Prenotazione cancellata · " + fullName,
                staffEmail("Prenotazione cancellata",
                        "Questa prenotazione è stata eliminata.",
                        row("Cliente", fullName)
                                + row("Giorno", r.getDate().format(df))
                                + row("Ora", r.getTime().format(tf)),
                        null, null));
    }

    public void notifyEventRegistrationCreated(EventRegistration reg) {
        java.time.LocalDateTime dt = reg.getEvent().getData();
        User user = reg.getUser();
        String firstName = user != null ? safe(user.getName()) : "-";
        String fullName = fullName(user);
        String phone = user != null ? safe(user.getPhone()) : "-";
        String eventTitle = safe(reg.getEvent().getTitolo());
        String when = dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String waMsg = "Ciao " + firstName
                + ", siamo il Corner Pub. Ti confermiamo l'iscrizione all'evento \"" + eventTitle
                + "\" del " + when + ". Ti aspettiamo in Piazza Duomo 58, Giovinazzo. A presto!";

        String rows = row("Evento", eventTitle)
                + row("Quando", when)
                + row("Cliente", fullName)
                + row("Telefono", phone)
                + row("Partecipanti", String.valueOf(reg.getPartecipanti()))
                + row("Note", reg.getNote())
                + row("Allergeni", reg.getAllergensNote());

        sendHtml("Nuova iscrizione evento · " + eventTitle + " · " + fullName,
                staffEmail("Nuova iscrizione evento",
                        "Qualcuno si è iscritto a un evento.",
                        rows, whatsAppLink(phone, waMsg),
                        "Apri WhatsApp e conferma al cliente"));
    }

    public void notifyEventRegistrationCancelled(EventRegistration reg) {
        sendHtml("Iscrizione evento cancellata · " + fullName(reg.getUser()),
                staffEmail("Iscrizione evento cancellata",
                        "Questa iscrizione è stata eliminata.",
                        row("Evento", safe(reg.getEvent().getTitolo()))
                                + row("Cliente", fullName(reg.getUser())),
                        null, null));
    }

    public void sendTestEmail() {
        sendHtml("Corner • Test mail prenotazioni",
                staffEmail("Test avviso prenotazioni",
                        "Se leggi questa mail, gli avvisi arrivano. Per il test vero fai una prenotazione dal sito.",
                        row("Destinazione", FALLBACK_ADMIN),
                        null, null));
    }

    private static String staffEmail(String title, String intro, String rows, String waLink, String waLabel) {
        String button = "";
        if (waLink != null && !waLink.isBlank() && !"#".equals(waLink)) {
            String label = waLabel == null ? "Apri WhatsApp" : waLabel;
            button = """
                    <p style="margin:24px 0 8px;font-size:14px;color:#555;">
                      Clicca il bottone: si apre WhatsApp già pronto con il messaggio di conferma. Controlla e invia.
                    </p>
                    <table cellpadding="0" cellspacing="0" role="presentation">
                      <tr>
                        <td style="background-color:#25D366;border-radius:8px;">
                          <a href="__WA_LINK__" style="display:inline-block;padding:14px 22px;color:#ffffff;text-decoration:none;font-weight:bold;font-size:16px;">
                            __WA_LABEL__
                          </a>
                        </td>
                      </tr>
                    </table>
                    """.replace("__WA_LINK__", waLink).replace("__WA_LABEL__", esc(label));
        }
        return """
                <html>
                <body style="margin:0;padding:0;background:#f4f4f4;">
                  <table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:24px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:10px;padding:28px 28px 24px;font-family:Arial,sans-serif;color:#222;line-height:1.5;">
                        <tr><td>
                          <p style="margin:0 0 4px;font-size:12px;letter-spacing:1px;color:#888;text-transform:uppercase;">Corner Pub Giovinazzo</p>
                          <h1 style="margin:0 0 12px;font-size:22px;color:#111;">__TITLE__</h1>
                          <p style="margin:0 0 18px;font-size:15px;color:#444;">__INTRO__</p>
                          <table width="100%" cellpadding="6" cellspacing="0" style="font-size:15px;">
                            __ROWS__
                          </table>
                          __BUTTON__
                          <p style="margin:28px 0 0;font-size:12px;color:#999;">Piazza Duomo 58, Giovinazzo · cornerpubgiovinazzo.com</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.replace("__TITLE__", esc(title))
                .replace("__INTRO__", esc(intro))
                .replace("__ROWS__", rows == null ? "" : rows)
                .replace("__BUTTON__", button);
    }

    private static String row(String label, String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return "";
        }
        return "<tr><td style=\"padding:6px 0;border-bottom:1px solid #eee;width:130px;color:#777;\">"
                + esc(label) + "</td><td style=\"padding:6px 0;border-bottom:1px solid #eee;font-weight:bold;\">"
                + esc(value) + "</td></tr>";
    }

    private static String fullName(User user) {
        if (user == null) {
            return "-";
        }
        String name = user.getName() == null ? "" : user.getName().trim();
        String surname = user.getSurname() == null ? "" : user.getSurname().trim();
        String joined = (name + " " + surname).trim();
        return joined.isBlank() ? "-" : joined;
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s.trim();
    }

    private static String esc(String s) {
        return safe(s)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
