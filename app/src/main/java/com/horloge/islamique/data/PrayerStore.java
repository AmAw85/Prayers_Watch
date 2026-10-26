package com.horloge.islamique.data;

import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Local persistence boundary. Implementations must preserve corrections on source updates. */
public interface PrayerStore {
    List<PrayerDay> loadRange(LocalDate from, LocalDate to);

    PrayerDay load(LocalDate date);

    void saveProviderDay(PrayerDay day);

    void saveCorrection(LocalDate date, Prayer prayer, Instant original,
                        Instant corrected, Instant correctedAt,
                        String reason, String provenance);

    void clearCorrection(LocalDate date, Prayer prayer);

    LocalDate latestStoredDate();
}
