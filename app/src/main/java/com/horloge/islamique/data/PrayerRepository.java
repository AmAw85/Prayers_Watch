package com.horloge.islamique.data;

import com.horloge.islamique.core.Correction;
import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.settings.SettingsRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Coordinates source fetching, local persistence and user corrections. */
public final class PrayerRepository {
    private final PrayerSource source;
    private final PrayerStore store;
    private final SettingsRepository settings;
    private final Clock clock;

    public PrayerRepository(PrayerSource source, PrayerStore store,
                            SettingsRepository settings, Clock clock) {
        this.source = source;
        this.store = store;
        this.settings = settings;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public PrayerSource source() {
        return source;
    }

    public SettingsRepository settings() {
        return settings;
    }

    public ZoneId zone() {
        return ZoneId.of(settings.timezoneId());
    }

    public City city() {
        return City.byId(settings.cityId());
    }

    /** Data produced for another city/timezone is never presented as current. */
    public PrayerDay load(LocalDate date) {
        PrayerDay day = store.load(date);
        return matchesCurrentLocation(day) ? day : null;
    }

    /** True when a stored record exists but belongs to another city or timezone. */
    public boolean hasMismatchedStoredData(LocalDate date) {
        PrayerDay day = store.load(date);
        return day != null && !matchesCurrentLocation(day);
    }

    /** Yesterday, today and tomorrow are needed for post-midnight Isha and the next Fajr. */
    public List<PrayerDay> loadContext(LocalDate today) {
        List<PrayerDay> days = new ArrayList<>();
        for (int offset = -1; offset <= 1; offset++) {
            PrayerDay day = load(today.plusDays(offset));
            if (day != null) {
                days.add(day);
            }
        }
        return days;
    }

    public List<PrayerDay> loadRange(LocalDate from, LocalDate to) {
        List<PrayerDay> result = new ArrayList<>();
        for (PrayerDay day : store.loadRange(from, to)) {
            if (matchesCurrentLocation(day)) {
                result.add(day);
            }
        }
        return result;
    }

    private boolean matchesCurrentLocation(PrayerDay day) {
        return day != null
                && day.locationId().equals(settings.cityId())
                && day.zone().equals(zone());
    }

    public SyncResult sync(LocalDate from, int numberOfDays) {
        if (SettingsRepository.PROVIDER_NONE.equals(settings.providerId())) {
            Instant at = clock.instant();
            settings.markSyncError(at, "no_provider");
            return SyncResult.failure("no_provider", at, 0);
        }
        Instant startedAt = clock.instant();
        settings.markSyncAttempt(startedAt);
        int saved = 0;
        for (int i = 0; i < numberOfDays; i++) {
            LocalDate date = from.plusDays(i);
            try {
                PrayerDay fetched = source.fetch(date, city(), zone());
                PrayerDay existing = store.load(date);
                if (!matchesCurrentLocation(existing)) {
                    existing = null;
                }
                PrayerDay merged = PrayerMerge.mergeProvider(existing, fetched);
                store.saveProviderDay(merged);
                saved++;
            } catch (DataSourceException ex) {
                settings.markSyncError(clock.instant(), ex.codeKey());
                return SyncResult.failure(ex.codeKey(), clock.instant(), saved);
            } catch (RuntimeException ex) {
                settings.markSyncError(clock.instant(), "storage");
                return SyncResult.failure("storage", clock.instant(), saved);
            }
        }
        Instant completedAt = clock.instant();
        settings.markSyncSuccess(completedAt);
        return SyncResult.success(saved, completedAt);
    }

    public PrayerDay saveCorrection(LocalDate date, Prayer prayer, Instant corrected,
                                    String reason, String provenance) {
        PrayerDay day = load(date);
        if (day == null) {
            return null;
        }
        Instant original = day.originalTimes().get(prayer);
        if (original == null) {
            return null;
        }
        Instant at = clock.instant();
        store.saveCorrection(date, prayer, original, corrected, at, reason, provenance);
        return load(date);
    }

    public PrayerDay clearCorrection(LocalDate date, Prayer prayer) {
        store.clearCorrection(date, prayer);
        return load(date);
    }

    public boolean hasUsableToday() {
        LocalDate today = LocalDate.now(clock.withZone(zone()));
        PrayerDay day = load(today);
        return day != null && day.hasAllPrayerTimes();
    }

    public boolean shouldAutoSync() {
        if (!SettingsRepository.PROVIDER_ALADHAN.equals(settings.providerId())) {
            return false;
        }
        Instant now = clock.instant();
        Instant attempt = settings.lastSyncAttempt();
        if (attempt != null
                && java.time.Duration.between(attempt, now).toMinutes() < 15) {
            return false;
        }
        if (!hasUsableToday()) {
            return true;
        }
        Instant success = settings.lastSuccessfulSync();
        return success == null
                || java.time.Duration.between(success, now).toHours() >= 12;
    }

    public List<Correction> corrections(LocalDate date) {
        PrayerDay day = load(date);
        if (day == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(day.corrections().values());
    }
}
