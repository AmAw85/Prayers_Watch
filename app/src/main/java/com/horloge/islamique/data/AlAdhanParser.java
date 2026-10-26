package com.horloge.islamique.data;

import com.horloge.islamique.core.HijriDate;
import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.ScheduleIssue;
import com.horloge.islamique.core.ScheduleValidator;
import com.horloge.islamique.core.SourceInfo;
import com.horloge.islamique.core.TimeEngine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Strict parser for the documented AlAdhan /v1/timings response. */
public final class AlAdhanParser {

    private AlAdhanParser() {
    }

    public static PrayerDay parse(String json, LocalDate requestedDate, ZoneId requestedZone,
                                  String locationId, SourceInfo source, Instant fetchedAt)
            throws DataSourceException {
        try {
            JSONObject root = new JSONObject(json);
            if (root.optInt("code", -1) != 200) {
                throw new DataSourceException(DataSourceException.Code.INVALID_DATA,
                        "API code was not 200");
            }
            JSONObject data = root.getJSONObject("data");
            JSONObject meta = data.optJSONObject("meta");
            if (meta == null) {
                meta = root.optJSONObject("meta");
            }
            if (meta != null) {
                String apiZone = meta.optString("timezone", "");
                if (!apiZone.isEmpty()) {
                    try {
                        if (!ZoneId.of(apiZone).equals(requestedZone)) {
                            throw new DataSourceException(DataSourceException.Code.TIMEZONE,
                                    "Provider timezone does not match configured timezone");
                        }
                    } catch (RuntimeException ex) {
                        throw new DataSourceException(DataSourceException.Code.TIMEZONE,
                                "Provider returned an invalid timezone", ex);
                    }
                }
            }

            JSONObject gregorian = data.getJSONObject("date").getJSONObject("gregorian");
            int day = Integer.parseInt(gregorian.getString("day"));
            int month = gregorian.getJSONObject("month").getInt("number");
            int year = Integer.parseInt(gregorian.getString("year"));
            if (day != requestedDate.getDayOfMonth()
                    || month != requestedDate.getMonthValue()
                    || year != requestedDate.getYear()) {
                throw new DataSourceException(DataSourceException.Code.INVALID_DATA,
                        "Provider returned a different Gregorian date");
            }

            JSONObject timings = data.getJSONObject("timings");
            Map<Prayer, Instant> times = new EnumMap<>(Prayer.class);
            List<ScheduleIssue> parseIssues = new ArrayList<>();
            Instant previous = null;
            for (Prayer prayer : Prayer.ALL) {
                String raw = timings.optString(apiKey(prayer), "");
                if (raw.isEmpty()) {
                    parseIssues.add(new ScheduleIssue(
                            ScheduleIssue.Code.MISSING_TIME, prayer.key()));
                    continue;
                }
                LocalTime localTime;
                try {
                    localTime = LocalTime.parse(raw.trim());
                } catch (DateTimeParseException ex) {
                    throw new DataSourceException(DataSourceException.Code.INVALID_DATA,
                            "Invalid time for " + prayer.key(), ex);
                }
                java.util.Optional<ScheduleIssue> localProblem =
                        TimeEngine.localTimeProblem(requestedDate, localTime, requestedZone);
                localProblem.ifPresent(parseIssues::add);

                Instant value = localTime.atDate(requestedDate).atZone(requestedZone).toInstant();
                if (previous != null && value.isBefore(previous)) {
                    if (prayer == Prayer.ISHA) {
                        // Some high-latitude schedules place Isha just after midnight.
                        value = value.plusSeconds(24 * 3600);
                    }
                }
                if (previous != null && value.equals(previous)) {
                    parseIssues.add(new ScheduleIssue(
                            ScheduleIssue.Code.DUPLICATE_TIME, prayer.key()));
                }
                times.put(prayer, value);
                previous = value;
            }

            HijriDate hijri = parseHijri(data.getJSONObject("date").optJSONObject("hijri"));
            List<ScheduleIssue> issues = ScheduleValidator.validateAll(
                    requestedDate, requestedZone, times,
                    java.util.Collections.emptyList(), java.util.Collections.emptyList(),
                    hijri, parseIssues);
            if (times.size() != Prayer.ALL.size()) {
                throw new DataSourceException(DataSourceException.Code.INVALID_DATA,
                        "The provider did not supply all five prayers");
            }
            for (ScheduleIssue issue : issues) {
                if (issue.isFatal()) {
                    throw new DataSourceException(DataSourceException.Code.INVALID_DATA,
                            "Invalid schedule: " + issue.code() + " " + issue.detail());
                }
            }
            return new PrayerDay(requestedDate, requestedZone, locationId, source, fetchedAt,
                    times, null, null, null, hijri, issues);
        } catch (JSONException ex) {
            throw new DataSourceException(DataSourceException.Code.INVALID_DATA,
                    "Malformed provider response", ex);
        } catch (NumberFormatException ex) {
            throw new DataSourceException(DataSourceException.Code.INVALID_DATA,
                    "Malformed date in provider response", ex);
        }
    }

    private static HijriDate parseHijri(JSONObject hijri) {
        if (hijri == null) {
            return null;
        }
        try {
            int day = Integer.parseInt(hijri.optString("day", "0"));
            int year = Integer.parseInt(hijri.optString("year", "0"));
            JSONObject month = hijri.optJSONObject("month");
            int monthNumber = month == null ? 0 : month.optInt("number", 0);
            String nameAr = month == null ? "" : month.optString("ar", "");
            String nameEn = month == null ? "" : month.optString("en", "");
            List<String> holidays = new ArrayList<>();
            JSONArray holidaysJson = hijri.optJSONArray("holidays");
            if (holidaysJson != null) {
                for (int i = 0; i < holidaysJson.length(); i++) {
                    String value = holidaysJson.optString(i, "");
                    if (!value.isEmpty()) {
                        holidays.add(value);
                    }
                }
            }
            return new HijriDate(day, monthNumber, year, nameAr, nameEn, holidays);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String apiKey(Prayer prayer) {
        switch (prayer) {
            case FAJR:
                return "Fajr";
            case DHUHR:
                return "Dhuhr";
            case ASR:
                return "Asr";
            case MAGHRIB:
                return "Maghrib";
            case ISHA:
                return "Isha";
            default:
                return prayer.key();
        }
    }
}
