package com.horloge.islamique.data;

import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.ScheduleIssue;
import com.horloge.islamique.core.ScheduleValidator;
import com.horloge.islamique.core.TimeInterval;

import java.util.Collections;
import java.util.List;

/**
 * Pure merge policy: a provider update may replace source values, but never removes
 * user corrections or explicitly supplied interval definitions.
 */
public final class PrayerMerge {

    private PrayerMerge() {
    }

    public static PrayerDay mergeProvider(PrayerDay existing, PrayerDay fresh) {
        if (existing == null) {
            return fresh;
        }
        List<TimeInterval> optional = fresh.optionalIntervals().isEmpty()
                ? existing.optionalIntervals() : fresh.optionalIntervals();
        List<TimeInterval> necessary = fresh.necessaryIntervals().isEmpty()
                ? existing.necessaryIntervals() : fresh.necessaryIntervals();
        List<ScheduleIssue> issues = ScheduleValidator.validateAll(
                fresh.date(), fresh.zone(), fresh.effectiveTimes(),
                optional, necessary, fresh.hijri(), fresh.issues());
        return new PrayerDay(
                fresh.date(), fresh.zone(), fresh.locationId(), fresh.source(), fresh.fetchedAt(),
                fresh.originalTimes(), existing.corrections(), optional, necessary,
                fresh.hijri(), issues);
    }

    public static PrayerDay withIntervals(PrayerDay day,
                                          List<TimeInterval> optional,
                                          List<TimeInterval> necessary,
                                          List<ScheduleIssue> inherited) {
        List<ScheduleIssue> issues = ScheduleValidator.validateAll(
                day.date(), day.zone(), day.effectiveTimes(),
                optional, necessary, day.hijri(), inherited);
        return day.withIntervals(optional, necessary, issues);
    }
}
