package com.horloge.islamique.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Locale-aware presentation helpers. All calculations remain in {@link TimeEngine}. */
public final class DateTimeText {

    private static final String[] HIJRI_MONTHS_FR = {
            "", "Mouharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani",
            "Joumada al-Oula", "Joumada al-Akhira", "Rajab", "Chaabane",
            "Ramadan", "Chawwal", "Dhou al-Qiada", "Dhou al-Hijja"
    };

    private static final String[] HIJRI_MONTHS_AR = {
            "", "محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى",
            "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    };

    private static final String[] HIJRI_MONTHS_EN = {
            "", "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani",
            "Jumada al-Ula", "Jumada al-Akhira", "Rajab", "Shaban",
            "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah"
    };

    private DateTimeText() {
    }

    public static Locale locale(String language) {
        if ("fr".equals(language)) {
            return Locale.FRENCH;
        }
        if ("en".equals(language)) {
            return Locale.ENGLISH;
        }
        return new Locale("ar");
    }

    public static String gregorianFull(LocalDate date, Locale locale) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(locale)
                .format(date);
    }

    public static String gregorianMedium(LocalDate date, Locale locale) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale)
                .format(date);
    }

    public static String monthTitle(LocalDate date, Locale locale) {
        return DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(date);
    }

    public static String weekdayShort(java.time.DayOfWeek day, Locale locale) {
        return DateTimeFormatter.ofPattern("EEE", locale).format(day);
    }

    public static String time(Instant instant, ZoneId zone, Locale locale) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(zone)
                .format(instant);
    }

    public static String timeWithSeconds(Instant instant, ZoneId zone, Locale locale) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(zone)
                .format(instant);
    }

    public static String dateTime(Instant instant, ZoneId zone, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(zone)
                .format(instant);
    }

    public static String hijri(HijriDate hijri, Locale locale) {
        if (hijri == null) {
            return "";
        }
        String language = locale.getLanguage();
        String month;
        if ("ar".equals(language)) {
            month = firstNonEmpty(hijri.monthNameAr(), monthAt(HIJRI_MONTHS_AR, hijri.month()));
        } else if ("fr".equals(language)) {
            month = monthAt(HIJRI_MONTHS_FR, hijri.month());
        } else {
            month = firstNonEmpty(hijri.monthNameEn(), monthAt(HIJRI_MONTHS_EN, hijri.month()));
        }
        if (month.isEmpty()) {
            month = Integer.toString(hijri.month());
        }
        String suffix = "ar".equals(language) ? "هـ" : ("fr".equals(language) ? "H" : "AH");
        String digits = localizeDigits(hijri.day() + " " + month + " " + hijri.year(), locale);
        return digits + " " + suffix;
    }

    public static String countdown(long totalSeconds) {
        return countdown(totalSeconds, Locale.ROOT);
    }

    public static String countdown(long totalSeconds, Locale locale) {
        long safe = Math.max(0, totalSeconds);
        long hours = safe / 3600;
        long minutes = (safe % 3600) / 60;
        long seconds = safe % 60;
        String raw;
        if (hours > 0) {
            raw = String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            raw = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        }
        return localizeDigits(raw, locale);
    }

    public static String dateIso(LocalDate date) {
        return date.toString();
    }

    public static String localizeDigits(String value, Locale locale) {
        if (!"ar".equals(locale.getLanguage())) {
            return value;
        }
        char[] arabic = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                result.append(arabic[c - '0']);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String monthAt(String[] values, int month) {
        return month >= 1 && month < values.length ? values[month] : "";
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second == null ? "" : second;
    }
}
