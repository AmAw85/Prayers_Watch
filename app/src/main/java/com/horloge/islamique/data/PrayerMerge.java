package com.horloge.islamique.data;

import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.ScheduleIssue;
import com.horloge.islamique.core.ScheduleValidator;

import java.util.List;

/**
 * Pure merge policy: a provider update may replace source values, but never removes
 * user corrections. The sunrise follows the source like the other solar data.
 */
public final class PrayerMerge {

    private PrayerMerge() {
    }

    public static PrayerDay mergeProvider(PrayerDay existing, PrayerDay fresh) {
        if (existing == null) {
            return fresh;
        }
        List<ScheduleIssue> issues = ScheduleValidator.validateAll(
                fresh.date(), fresh.zone(), fresh.effectiveTimes(),
                fresh.hijri(), fresh.issues());
        return new PrayerDay(
                fresh.date(), fresh.zone(), fresh.locationId(), fresh.source(), fresh.fetchedAt(),
                fresh.sunrise(), fresh.originalTimes(), existing.corrections(), issues,
                fresh.hijri());
    }
}
