package com.horloge.islamique.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.horloge.islamique.core.Correction;
import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.SourceInfo;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

/** Ensures a provider update never removes user corrections or sunrise. */
public final class PrayerMergeTest {
    private static final ZoneId ZONE = ZoneId.of("Africa/Algiers");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 26);

    @Test
    public void mergeProvider_keepsCorrectionsAndTakesFreshSunrise() {
        Instant originalFajr = at(2026, 10, 26, 5, 39);
        Instant correctedFajr = at(2026, 10, 26, 5, 32);
        Correction correction = new Correction(Prayer.FAJR, originalFajr, correctedFajr,
                at(2026, 10, 26, 6, 0),
                "Observation locale", "Mosquée voisine");
        PrayerDay existing = new PrayerDay(DATE, ZONE, "algiers", source(),
                at(2026, 10, 26, 4, 0), null,
                originalTimes(originalFajr), null, null, null);
        PrayerDay existingWithCorrection = existing.withCorrection(correction);

        Map<Prayer, Instant> freshTimes = originalTimes(at(2026, 10, 26, 5, 29));
        Instant freshSunrise = at(2026, 10, 26, 7, 4);
        PrayerDay fresh = new PrayerDay(DATE, ZONE, "algiers", source(),
                at(2026, 10, 26, 7, 0), freshSunrise,
                freshTimes, null, null, null);

        PrayerDay merged = PrayerMerge.mergeProvider(existingWithCorrection, fresh);

        assertEquals(freshSunrise, merged.sunrise());
        assertEquals(correctedFajr, merged.effectiveTimes().get(Prayer.FAJR));
        assertEquals(Prayer.ALL.size(), merged.effectiveTimes().size());
        assertTrue(merged.corrections().containsKey(Prayer.FAJR));
        assertFalse(merged.hasFatalIssue());
    }

    private static Map<Prayer, Instant> originalTimes(Instant fajr) {
        Map<Prayer, Instant> result = new EnumMap<>(Prayer.class);
        result.put(Prayer.FAJR, fajr);
        result.put(Prayer.DHUHR, at(2026, 10, 26, 12, 32));
        result.put(Prayer.ASR, at(2026, 10, 26, 15, 34));
        result.put(Prayer.MAGHRIB, at(2026, 10, 26, 17, 58));
        result.put(Prayer.ISHA, at(2026, 10, 26, 19, 19));
        return result;
    }

    private static SourceInfo source() {
        return new SourceInfo("aladhan", "AlAdhan",
                SourceInfo.Kind.CALCULATED, "AlAdhan");
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(ZONE).toInstant();
    }
}
