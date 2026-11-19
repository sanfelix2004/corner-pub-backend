package com.corner.pub.service;

import com.corner.pub.model.Event;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class RecurrenceSupport {

    public static final int UPCOMING_LIMIT = 52;
    private static final ZoneId ROME = ZoneId.of("Europe/Rome");

    private RecurrenceSupport() {
    }

    private static LocalDateTime nowRome() {
        return LocalDateTime.now(ROME);
    }

    private static LocalDate todayRome() {
        return LocalDate.now(ROME);
    }

    public static List<DayOfWeek> parseDays(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<DayOfWeek> days = new ArrayList<>();
        for (String part : raw.split("[,;\\s]+")) {
            DayOfWeek day = parseOne(part.trim());
            if (day != null && !days.contains(day)) {
                days.add(day);
            }
        }
        days.sort(Comparator.comparingInt(DayOfWeek::getValue));
        return List.copyOf(days);
    }

    public static String serialize(List<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
                .map(day -> String.valueOf(day.getValue()))
                .collect(Collectors.joining(","));
    }

    public static String serialize(String raw) {
        return serialize(parseDays(raw));
    }

    public static LocalDateTime firstOccurrence(LocalDate start, LocalTime time, String giorniSettimana) {
        LocalDate from = start != null ? start : todayRome();
        LocalTime at = time != null ? time : LocalTime.of(20, 0);
        List<DayOfWeek> days = parseDays(giorniSettimana);
        if (days.isEmpty()) {
            return from.atTime(at);
        }
        for (int i = 0; i < 7; i++) {
            LocalDate candidate = from.plusDays(i);
            if (days.contains(candidate.getDayOfWeek())) {
                return candidate.atTime(at);
            }
        }
        return from.atTime(at);
    }

    public static List<LocalDate> upcoming(Event event, int limit) {
        if (event == null || event.getData() == null) {
            return List.of();
        }
        return upcoming(
                event.getData(),
                event.getDataFine(),
                parseDays(event.effectiveGiorniSettimana()),
                event.isRicorrente(),
                limit);
    }

    public static List<LocalDate> upcoming(
            LocalDateTime first,
            LocalDate dataFine,
            List<DayOfWeek> days,
            boolean ricorrente,
            int limit
    ) {
        if (first == null) {
            return List.of();
        }
        LocalDateTime now = nowRome();
        LocalDate today = now.toLocalDate();
        LocalDate start = first.toLocalDate();
        if (!ricorrente || days == null || days.isEmpty()) {
            return first.isBefore(now) ? List.of() : List.of(start);
        }
        LocalDate cursor = today.isAfter(start) ? today : start;
        List<LocalDate> out = new ArrayList<>();
        int guard = 0;
        while (out.size() < Math.max(1, limit) && guard++ < 800) {
            if (dataFine != null && cursor.isAfter(dataFine)) {
                break;
            }
            if (!cursor.isBefore(start) && days.contains(cursor.getDayOfWeek())) {
                LocalDateTime lastSlot = cursor.atTime(LocalTime.of(23, 0));
                if (!lastSlot.isBefore(now)) {
                    out.add(cursor);
                }
            }
            cursor = cursor.plusDays(1);
        }
        return out;
    }

    public static boolean isValidOccurrence(Event event, LocalDate date) {
        if (event == null || event.getData() == null || date == null) {
            return false;
        }
        LocalDate start = event.getData().toLocalDate();
        if (date.isBefore(start)) {
            return false;
        }
        if (event.getDataFine() != null && date.isAfter(event.getDataFine())) {
            return false;
        }
        List<DayOfWeek> days = parseDays(event.effectiveGiorniSettimana());
        if (!event.isRicorrente() || days.isEmpty()) {
            return date.equals(start);
        }
        return days.contains(date.getDayOfWeek());
    }

    public static boolean isStillBookable(Event event, LocalDate today) {
        if (event == null || event.getData() == null) {
            return false;
        }
        LocalDate day = today != null ? today : todayRome();
        if (event.isRicorrente()) {
            return event.getDataFine() == null || !event.getDataFine().isBefore(day);
        }
        return !event.getData().toLocalDate().isBefore(day);
    }

    public static String label(boolean ricorrente, String giorniSettimana, LocalDate start, LocalDate end) {
        List<DayOfWeek> days = parseDays(giorniSettimana);
        String dayPart = formatDays(days);
        boolean forever = end == null;
        if (ricorrente && !dayPart.isEmpty()) {
            return forever ? dayPart + " · per sempre" : dayPart;
        }
        if (start != null && forever) {
            return dayPart.isEmpty() ? "Dal " + formatDate(start) + " · per sempre"
                    : "Dal " + formatDate(start) + " · " + dayPart + " · per sempre";
        }
        if (start != null && end != null) {
            String range = formatDate(start) + " – " + formatDate(end);
            return dayPart.isEmpty() ? range : range + " · " + dayPart;
        }
        return dayPart;
    }

    public static String formatDays(List<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return "";
        }
        List<String> names = days.stream()
                .map(day -> day.getDisplayName(TextStyle.FULL, Locale.ITALIAN))
                .toList();
        if (names.size() == 1) {
            return "Ogni " + names.get(0);
        }
        if (names.size() == 2) {
            return "Ogni " + names.get(0) + " e " + names.get(1);
        }
        return "Ogni " + String.join(", ", names.subList(0, names.size() - 1))
                + " e " + names.get(names.size() - 1);
    }

    public static LocalDateTime atEventTime(Event event, LocalDate occurrence) {
        return atEventTime(event, occurrence, null);
    }

    public static LocalDateTime atEventTime(Event event, LocalDate occurrence, LocalTime time) {
        LocalTime at = time;
        if (at == null && event != null && event.getData() != null) {
            at = event.getData().toLocalTime();
        }
        if (occurrence != null && at != null) {
            return occurrence.atTime(at);
        }
        if (event != null && event.getData() != null) {
            return event.getData();
        }
        return occurrence != null ? occurrence.atStartOfDay() : null;
    }

    private static DayOfWeek parseOne(String part) {
        if (part == null || part.isBlank()) {
            return null;
        }
        try {
            int n = Integer.parseInt(part);
            if (n >= 1 && n <= 7) {
                return DayOfWeek.of(n);
            }
        } catch (NumberFormatException ignored) {
        }
        String value = part.toLowerCase(Locale.ITALIAN)
                .replace("ì", "i")
                .replace("é", "e");
        return switch (value) {
            case "lun", "lunedi", "monday", "mon" -> DayOfWeek.MONDAY;
            case "mar", "martedi", "tuesday", "tue" -> DayOfWeek.TUESDAY;
            case "mer", "mercoledi", "wednesday", "wed" -> DayOfWeek.WEDNESDAY;
            case "gio", "giovedi", "thursday", "thu" -> DayOfWeek.THURSDAY;
            case "ven", "venerdi", "friday", "fri" -> DayOfWeek.FRIDAY;
            case "sab", "sabato", "saturday", "sat" -> DayOfWeek.SATURDAY;
            case "dom", "domenica", "sunday", "sun" -> DayOfWeek.SUNDAY;
            default -> {
                try {
                    yield DayOfWeek.valueOf(part.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }

    private static String formatDate(LocalDate date) {
        return date.getDayOfMonth() + " "
                + date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ITALIAN);
    }
}
