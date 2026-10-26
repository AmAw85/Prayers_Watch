package com.horloge.islamique.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Pure JVM coverage of the new interval rule: Fajr ends at the source sunrise,
 * the other prayers end at the next prayer entry, and Isha ends at the next day's
 * Fajr when it is available.
 */
public final class TimeEnginePrayerIntervalsTest {
    private static final ZoneId ZONE = ZoneId.of("Africa/Algiers");

    @Test
    public void prayerIntervals_useActualSourceTimesIncludingSunrise() {
        PrayerDay day = day(2026, 10, 26, 5, 39, 7, 5, 12, 32, 15, 34, 17, 58, 19, 19);
        PrayerDay nextDay = day(2026, 10, 27, 5, 40, 7, 6, 12, 32, 15, 33, 17, 57, 19, 18);

        List<TimeInterval> intervals = TimeEngine.prayerIntervals(day, nextDay);

        assertEquals(5, intervals.size());
        assertInterval(intervals.get(0), Prayer.FAJR, 5, 39, 7, 5);
        assertInterval(intervals.get(1), Prayer.DHUHR, 12, 32, 15, 34);
        assertInterval(intervals.get(2), Prayer.ASR, 15, 34, 17, 58);
        assertInterval(intervals.get(3), Prayer.MAGHRIB, 17, 58, 19, 19);
        assertInterval(intervals.get(4), Prayer.ISHA, 19, 19, 5, 40, 27);
    }

    @Test
    public void isha_interval_crossesMidnightToEndOfNextFajr() {
        PrayerDay day = day(2026, 10, 26, 5, 39, 7, 5, 12, 32, 15, 34, 17, 58, 23, 45);
        PrayerDay nextDay = day(2026, 10, 27, 5, 40, 7, 6, 12, 32, 15, 33, 17, 57, 19, 18);

        List<TimeInterval> intervals = TimeEngine.prayerIntervals(day, nextDay);
        TimeInterval isha = intervals.get(intervals.size() - 1);

        assertEquals("prayer-isha", isha.id());
        assertEquals(LocalDateTime.of(2026, 10, 26, 23, 45).atZone(ZONE).toInstant(),
                isha.start());
        assertEquals(LocalDateTime.of(2026, 10, 27, 5, 40).atZone(ZONE).toInstant(),
                isha.end());
    }

    @Test
    public void missingSunriseOrNextDayDoesNotInventAnInterval() {
        Map<Prayer, Instant> times = amd(2026, 10, 26, true);
        times.put(Prayer.FAJR, at(2026, 10, 26, 5, 39));
        times.put(Prayer.DHUHR, at(2026, 10, 26, 12, 32));
        times.put(Prayer.ASR, at(2026, 10, 26, 15, 34));
        times.put(Prayer.MAGHRIB, at(2026, 10, 26, 17, 58));
        times.put(Prayer.ISHA, at(2026, 10, 26, 19, 19));
        // No sunrise and no next day.
        PrayerDay day = new PrayerDay(LocalDate.of(2026, 10, 26), ZONE, "algiers",
                source(), Instant.now(), null, times, null, null, null);

        List<TimeInterval> intervals = TimeEngine.prayerIntervals(day, null);
        assertFalse(hasPrayer(intervals, Prayer.FAJR));
        assertFalse(hasPrayer(intervals, Prayer.ISHA));
        assertTrue(hasPrayer(intervals, Prayer.DHUHR));
        assertEquals(3, intervals.size());
    }

    @Test
    public void invertedBoundsAreRejected() {
        PrayerDay day = day(2026, 10, 26, 8, 0, 6, 0, 12, 32, 15, 34, 17, 58, 19, 19);
        PrayerDay next = day(2026, 10, 27, 5, 40, 7, 6, 12, 32, 15, 33, 17, 57, 19, 18);

        List<TimeInterval> intervals = TimeEngine.prayerIntervals(day, next);
        assertFalse(hasPrayer(intervals, Prayer.FAJR));
        assertTrue(hasPrayer(intervals, Prayer.DHUHR));
    }

    @Test
    public void currentPrayer_matchesDerivedIntervalsOnly() {
        PrayerDay day = day(2026, 10, 26, 5, 39, 7, 5, 12, 32, 15, 34, 17, 58, 19, 19);
        PrayerDay nextDay = day(2026, 10, 27, 5, 40, 7, 6, 12, 32, 15, 33, 17, 57, 19, 18);
        List<PrayerDay> days = List.of(day, nextDay);

        Instant afterSunrise = at(2026, 10, 26, 8, 0);
        Instant duringDhuhr = at(2026, 10, 26, 13, 0);
        Instant duringIsha = at(2026, 10, 26, 23, 0);

        assertTrue(TimeEngine.currentPrayer(afterSunrise, days).isEmpty());
        assertEquals(Prayer.DHUHR,
                TimeEngine.currentPrayer(duringDhuhr, days).get().prayer);
        assertEquals(Prayer.ISHA,
                TimeEngine.currentPrayer(duringIsha, days).get().prayer);
    }

    private static PrayerDay day(int year, int month, int dayOfWeek,
                                 int fH, int fM, int srH, int srM,
                                 int dH, int dM, int aH, int aM,
                                 int mH, int mM, int iH, int iM) {
        Map<Prayer, Instant> times = amd(year, month, dayOfWeek, true);
        Instant sunrise = at(year, month, dayOfWeek, srH, srM);
        times.put(Prayer.FAJR, at(year, month, dayOfWeek, fH, fM));
        times.put(Prayer.DHUHR, at(year, month, dayOfWeek, dH, dM));
        times.put(Prayer.ASR, at(year, month, dayOfWeek, aH, aM));
        times.put(Prayer.MAGHRIB, at(year, month, dayOfWeek, mH, mM));
        times.put(Prayer.ISHA, at(year, month, dayOfWeek, iH, iM));
        return new PrayerDay(LocalDate.of(year, month, dayOfWeek), ZONE, "algiers",
                source(), Instant.now(), sunrise, times, null, null, null);
    }

    private static Map<Prayer, Instant> amd(int year, int month, int day, boolean unused) {
        return new EnumMap<>(Prayer.class);
    }

    private static SourceInfo source() {
        return new SourceInfo("aladhan", "AlAdhan", SourceInfo.Kind.CALCULATED, "AlAdhan");
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(ZONE).toInstant();
    }

    private static void assertInterval(TimeInterval interval, Prayer prayer,
                                       int startHour, int startMinute,
                                       int endHour, int endMinute) {
        assertEquals("prayer-" + prayer.key(), interval.id());
        assertEquals(at(2026, 10, 26, startHour, startMinute), interval.start());
        assertEquals(at(2026, 10, 26, endHour, endMinute), interval.end());
    }

    private static void assertInterval(TimeInterval interval, Prayer prayer,
                                       int startHour, int startMinute,
                                       int endHour, int endMinute, int endDay) {
        assertEquals("prayer-" + prayer.key(), interval.id());
        assertEquals(at(2026, 10, 26, startHour, startMinute), interval.start());
        assertEquals(at(2026, 10, endDay, endHour, endMinute), interval.end());
    }

    private static boolean hasPrayer(List<TimeInterval> intervals, Prayer prayer) {
        for (TimeInterval interval : intervals) {
            if (("prayer-" + prayer.key()).equals(interval.id())) {
                return true;
            }
        }
        return false;
    }
}
