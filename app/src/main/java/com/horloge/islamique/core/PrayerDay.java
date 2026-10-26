package com.horloge.islamique.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One local day of prayer data. Source values are kept separately from corrections.
 * Optional/necessary religious intervals are only present when supplied explicitly.
 */
public final class PrayerDay {
    private final LocalDate date;
    private final ZoneId zone;
    private final String locationId;
    private final SourceInfo source;
    private final Instant fetchedAt;
    private final Map<Prayer, Instant> originalTimes;
    private final Map<Prayer, Correction> corrections;
    private final List<TimeInterval> optionalIntervals;
    private final List<TimeInterval> necessaryIntervals;
    private final HijriDate hijri;
    private final List<ScheduleIssue> issues;

    public PrayerDay(LocalDate date, ZoneId zone, String locationId, SourceInfo source,
                     Instant fetchedAt,
                     Map<Prayer, Instant> originalTimes, Map<Prayer, Correction> corrections,
                     List<TimeInterval> optionalIntervals,
                     List<TimeInterval> necessaryIntervals,
                     HijriDate hijri, List<ScheduleIssue> issues) {
        this.date = date;
        this.zone = zone;
        this.locationId = locationId == null ? "" : locationId;
        this.source = source;
        this.fetchedAt = fetchedAt;
        this.originalTimes = Collections.unmodifiableMap(
                new EnumMap<>(originalTimes == null ? new EnumMap<Prayer, Instant>(Prayer.class) : originalTimes));
        this.corrections = Collections.unmodifiableMap(
                corrections == null ? new EnumMap<>(Prayer.class) : new EnumMap<>(corrections));
        this.optionalIntervals = optionalIntervals == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(optionalIntervals));
        this.necessaryIntervals = necessaryIntervals == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(necessaryIntervals));
        this.hijri = hijri;
        this.issues = issues == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(issues));
    }

    public LocalDate date() {
        return date;
    }

    public ZoneId zone() {
        return zone;
    }

    public String locationId() {
        return locationId;
    }

    public SourceInfo source() {
        return source;
    }

    public Instant fetchedAt() {
        return fetchedAt;
    }

    public Map<Prayer, Instant> originalTimes() {
        return originalTimes;
    }

    public Map<Prayer, Instant> effectiveTimes() {
        Map<Prayer, Instant> result = new LinkedHashMap<>();
        for (Prayer prayer : Prayer.ALL) {
            Instant original = originalTimes.get(prayer);
            if (original == null) {
                continue;
            }
            Correction correction = corrections.get(prayer);
            result.put(prayer, correction == null ? original : correction.corrected());
        }
        return result;
    }

    public Map<Prayer, Correction> corrections() {
        return corrections;
    }

    public List<TimeInterval> optionalIntervals() {
        return optionalIntervals;
    }

    public List<TimeInterval> necessaryIntervals() {
        return necessaryIntervals;
    }

    public HijriDate hijri() {
        return hijri;
    }

    public List<ScheduleIssue> issues() {
        return issues;
    }

    public boolean hasFatalIssue() {
        for (ScheduleIssue issue : issues) {
            if (issue.isFatal()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllPrayerTimes() {
        return effectiveTimes().size() == Prayer.ALL.size();
    }

    public DataFreshness freshness(Instant now) {
        if (!hasAllPrayerTimes()) {
            return DataFreshness.UNAVAILABLE;
        }
        if (hasFatalIssue() || hijri == null || !hijri.isPlausible()) {
            return DataFreshness.UNVERIFIED;
        }
        if (fetchedAt == null) {
            return DataFreshness.UNVERIFIED;
        }
        long ageHours = java.time.Duration.between(fetchedAt, now).toHours();
        if (ageHours < 0) {
            // A clock change must not turn fresh data into a false failure.
            return DataFreshness.CURRENT;
        }
        return ageHours <= 48 ? DataFreshness.CURRENT : DataFreshness.STALE;
    }

    public PrayerDay withCorrection(Correction correction) {
        Map<Prayer, Correction> updated = new EnumMap<>(Prayer.class);
        updated.putAll(corrections);
        updated.put(correction.prayer(), correction);
        return new PrayerDay(date, zone, locationId, source, fetchedAt, originalTimes, updated,
                optionalIntervals, necessaryIntervals, hijri, issues);
    }

    public PrayerDay withoutCorrection(Prayer prayer) {
        Map<Prayer, Correction> updated = new EnumMap<>(Prayer.class);
        updated.putAll(corrections);
        updated.remove(prayer);
        return new PrayerDay(date, zone, locationId, source, fetchedAt, originalTimes, updated,
                optionalIntervals, necessaryIntervals, hijri, issues);
    }

    public PrayerDay withIntervals(List<TimeInterval> optional, List<TimeInterval> necessary,
                                   List<ScheduleIssue> newIssues) {
        return new PrayerDay(date, zone, locationId, source, fetchedAt, originalTimes, corrections,
                optional, necessary, hijri, newIssues);
    }

    public enum DataFreshness {
        CURRENT,
        STALE,
        UNAVAILABLE,
        UNVERIFIED
    }
}
