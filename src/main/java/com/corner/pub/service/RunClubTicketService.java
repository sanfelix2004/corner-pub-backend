package com.corner.pub.service;

import com.corner.pub.exception.CornerPubException;
import com.corner.pub.model.Event;
import com.corner.pub.model.EventRegistration;
import com.corner.pub.model.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

@Service
public class RunClubTicketService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'alle' HH:mm", Locale.ITALIAN);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String publicBaseUrl;

    public RunClubTicketService(
            @Value("${app.public-base-url:https://cornerpubgiovinazzo.com}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    public byte[] render(EventRegistration registration) {
        Event event = registration.getEvent();
        User user = registration.getUser();
        if (event == null || user == null || registration.getTicketCode() == null) {
            throw new CornerPubException("Pass non disponibile.");
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A5);
            doc.addPage(page);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();

            byte[] qrPng = qrPng(registration.getTicketCode());
            PDImageXObject qr = PDImageXObject.createFromByteArray(doc, qrPng, "qr");
            PDImageXObject logo = loadLogo(doc);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                fill(cs, new Color(243, 236, 223), 0, 0, width, height);
                fill(cs, new Color(124, 28, 45), 0, height - 78, width, 78);
                checker(cs, 18, height - 96, width - 36);

                float logoSize = 88;
                float logoX = (width - logoSize) / 2f;
                float logoY = height - 78 - 8 - logoSize;
                if (logo != null) {
                    cs.drawImage(logo, logoX, logoY, logoSize, logoSize);
                }

                float y = logoY - 28;
                y = center(cs, PDType1Font.HELVETICA_BOLD, 13, y, width, new Color(124, 28, 45), "PASS DI ISCRIZIONE");
                String fullName = (safe(user.getName()) + " " + safe(user.getSurname())).trim();
                y = center(cs, PDType1Font.HELVETICA_BOLD, 18, y - 8, width, new Color(28, 20, 18), fullName);
                y = center(cs, PDType1Font.HELVETICA_BOLD, 14, y - 10, width, new Color(28, 20, 18), safe(event.getTitolo()));

                String when = event.getData() == null ? "-" : event.getData().format(WHEN);
                y = line(cs, 36, y - 16, width - 72, "Quando", when);
                y = line(cs, 36, y - 6, width - 72, "Ritrovo", safe(event.getLuogo()));
                String pace = join(safe(event.getDistanza()), safe(event.getRitmo()));
                y = line(cs, 36, y - 4, width - 72, "Uscita", pace);
                y = line(cs, 36, y - 2, width - 72, "Passo", safe(registration.getPaceCategory()));
                y = line(cs, 36, y - 2, width - 72, "Quota", quotaLabel(event, registration));

                if (event.isAnnullata()) {
                    y -= 16;
                    center(cs, PDType1Font.HELVETICA_BOLD, 12, y, width, new Color(164, 32, 32), "USCITA ANNULLATA");
                }

                float qrSize = 112;
                float qrX = (width - qrSize) / 2f;
                float qrY = 96;
                cs.drawImage(qr, qrX, qrY, qrSize, qrSize);
                center(cs, PDType1Font.HELVETICA_BOLD, 13, qrY - 14, width, new Color(28, 20, 18), registration.getTicketCode());

                String accepted = registration.getCreatedAt() == null
                        ? ""
                        : registration.getCreatedAt().format(SHORT);
                String policy = "Regolamento " + safe(registration.getRulesVersion())
                        + " e privacy " + safe(registration.getPrivacyPolicyVersion())
                        + (accepted.isBlank() ? "" : " accettati il " + accepted)
                        + ". Foto: " + (Boolean.TRUE.equals(registration.getPhotoConsent()) ? "si" : "no")
                        + ".";
                drawWrapped(cs, PDType1Font.HELVETICA, 7.5f, 28, 58, width - 56, new Color(80, 62, 58), policy);

                fill(cs, new Color(76, 16, 25), 0, 0, width, 36);
                center(cs, PDType1Font.HELVETICA, 8, 20, width, Color.WHITE, "Corner Pub  ·  Piazza Duomo 58, Giovinazzo");
                center(cs, PDType1Font.HELVETICA, 8, 9, width, new Color(224, 161, 27), publicBaseUrl.replace("https://", "").replace("http://", ""));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (CornerPubException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CornerPubException("Impossibile creare il PDF del pass.");
        }
    }

    private byte[] qrPng(String code) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 360, 360, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private PDImageXObject loadLogo(PDDocument doc) {
        try (InputStream in = getClass().getResourceAsStream("/static/images/corner-run-club-logo.jpg")) {
            if (in == null) {
                return null;
            }
            return PDImageXObject.createFromByteArray(doc, in.readAllBytes(), "logo");
        } catch (Exception ex) {
            return null;
        }
    }

    private void fill(PDPageContentStream cs, Color color, float x, float y, float w, float h) throws Exception {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void checker(PDPageContentStream cs, float x, float y, float width) throws Exception {
        float size = 8;
        int cols = (int) (width / size);
        for (int i = 0; i < cols; i++) {
            fill(cs, i % 2 == 0 ? new Color(124, 28, 45) : new Color(243, 236, 223), x + i * size, y, size, size);
        }
    }

    private float center(PDPageContentStream cs, PDType1Font font, float size, float y, float pageWidth, Color color, String text) throws Exception {
        String value = pdf(text);
        float textWidth = font.getStringWidth(value) / 1000f * size;
        float x = Math.max(16, (pageWidth - textWidth) / 2f);
        textAt(cs, font, size, x, y, color, value);
        return y - size;
    }

    private String quotaLabel(Event event, EventRegistration registration) {
        int quota = event.getQuotaEuro() == null ? 0 : event.getQuotaEuro();
        if (quota <= 0) {
            return "Gratis";
        }
        if ("PAID".equals(registration.getPaymentStatus())) {
            return quota + " euro, gia pagati";
        }
        return quota + " euro da pagare al ritrovo";
    }

    private float line(PDPageContentStream cs, float x, float y, float width, String label, String value) throws Exception {
        textAt(cs, PDType1Font.HELVETICA_BOLD, 9, x, y, new Color(124, 28, 45), pdf(label).toUpperCase(Locale.ITALIAN));
        float labelWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(pdf(label).toUpperCase(Locale.ITALIAN)) / 1000f * 9f;
        String shown = pdf(value);
        float max = width - labelWidth - 8;
        while (shown.length() > 3 && PDType1Font.HELVETICA.getStringWidth(shown) / 1000f * 11f > max) {
            shown = shown.substring(0, shown.length() - 2);
        }
        if (!shown.equals(pdf(value)) && shown.length() > 3) {
            shown = shown.substring(0, shown.length() - 1) + "...";
        }
        textAt(cs, PDType1Font.HELVETICA, 11, x + labelWidth + 8, y - 1, new Color(28, 20, 18), shown);
        return y - 16;
    }

    private void drawWrapped(PDPageContentStream cs, PDType1Font font, float size, float x, float y, float width, Color color, String text) throws Exception {
        String[] words = pdf(text).split(" ");
        StringBuilder line = new StringBuilder();
        float cursor = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float candidateWidth = font.getStringWidth(candidate) / 1000f * size;
            if (candidateWidth > width && !line.isEmpty()) {
                textAt(cs, font, size, x, cursor, color, line.toString());
                cursor -= size + 2;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            textAt(cs, font, size, x, cursor, color, line.toString());
        }
    }

    private void textAt(PDPageContentStream cs, PDType1Font font, float size, float x, float y, Color color, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private String join(String distanza, String ritmo) {
        boolean hasDistanza = distanza != null && !distanza.equals("-");
        boolean hasRitmo = ritmo != null && !ritmo.equals("-");
        if (hasDistanza && hasRitmo) {
            return distanza + "  ·  " + ritmo;
        }
        if (hasDistanza) {
            return distanza;
        }
        if (hasRitmo) {
            return ritmo;
        }
        return "-";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String pdf(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        String cleaned = value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        for (int i = 0; i < cleaned.length(); ) {
            int cp = cleaned.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            i += Character.charCount(cp);
            try {
                PDType1Font.HELVETICA.encode(ch);
                sb.append(ch);
            } catch (Exception ex) {
                if (cp == '’' || cp == '‘') {
                    sb.append('\'');
                } else if (cp == '“' || cp == '”') {
                    sb.append('"');
                } else if (cp == '–' || cp == '—') {
                    sb.append('-');
                } else {
                    sb.append(' ');
                }
            }
        }
        String out = sb.toString().replaceAll(" +", " ").trim();
        return out.isEmpty() ? "-" : out;
    }
}
