package com.horloge.islamique.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.SourceInfo;

import org.junit.Test;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;

/** Parses the documented AlAdhan response and checks the persisted sunrise. */
public final class AlAdhanParserSunriseTest {
    private static final ZoneId ZONE = ZoneId.of("Africa/Algiers");

    @Test
    public void parsesSunriseAsSeparateSolarEvent() throws Exception {
        PrayerDay day = parseFixture();

        assertNotNull(day.sunrise());
        assertEquals(LocalDateTime.of(2026, 10, 26, 7, 5).atZone(ZONE).toInstant(),
                day.sunrise());
        assertEquals(ZONE, day.zone());
        assertEquals(LocalDate.of(2026, 10, 26), day.date());
        assertNotNull(day.effectiveTimes().get(Prayer.MAGHRIB));
        assertNotNull(day.effectiveTimes().get(Prayer.FAJR));
    }

    private PrayerDay parseFixture() throws Exception {
        try (InputStream in = getClass()
                .getResourceAsStream("/aladhan-algiers-2026-10-26.json")) {
            assertNotNull("fixture missing", in);
            byte[] bytes = in.readAllBytes();
            String json = new String(bytes, StandardCharsets.UTF_8).trim();
            return AlAdhanParser.parse(json,
                    LocalDate.of(2026, 10, 26), ZONE, "algiers",
                    new SourceInfo("aladhan", "AlAdhan",
                            SourceInfo.Kind.CALCULATED, "AlAdhan"),
                    Instant.now());
        }
    }
}
