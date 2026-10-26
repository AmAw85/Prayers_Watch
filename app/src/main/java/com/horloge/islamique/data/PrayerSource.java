package com.horloge.islamique.data;

import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.SourceInfo;

import java.time.LocalDate;
import java.time.ZoneId;

/** A source of daily prayer data. UI and temporal code depend on this abstraction. */
public interface PrayerSource {
    SourceInfo info();

    PrayerDay fetch(LocalDate date, City city, ZoneId zone) throws DataSourceException;
}
