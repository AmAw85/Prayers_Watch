package com.horloge.islamique.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates schedule data before and after storage. */
public final class ScheduleValidator {

    private ScheduleValidator() {
    }

    public static List<ScheduleIssue> validateTimes(
            LocalDate date, ZoneId zone, Map<Prayer, Instant> times) {
        List<ScheduleIssue> issues = new ArrayList<>();
        if (times == null) {
            issues.add(new ScheduleIssue(ScheduleIssue.Code.MISSING_TIME, "all"));
            return issues;
        }
        Set<Instant> distinct = new HashSet<>();
        Instant previous = null;
        for (Prayer prayer : Prayer.ALL) {
            Instant value = times.get(prayer);
            if (value == null) {
                issues.add(new ScheduleIssue(ScheduleIssue.Code.MISSING_TIME, prayer.key()));
                continue;
            }
            if (!distinct.add(value)) {
                issues.add(new ScheduleIssue(ScheduleIssue.Code.DUPLICATE_TIME, prayer.key()));
            }
            if (previous != null && !value.isAfter(previous)) {
                issues.add(new ScheduleIssue(ScheduleIssue.Code.NON_CHRONOLOGICAL, prayer.key()));
            }
            previous = value;
        }
        if (date == null || zone == null) {
            issues.add(new ScheduleIssue(ScheduleIssue.Code.NONEXISTENT_LOCAL_TIME, "date/zone"));
        }
        return issues;
    }

    public static List<ScheduleIssue> validateIntervals(List<TimeInterval> intervals) {
        List<ScheduleIssue> issues = new ArrayList<>();
        if (intervals == null) {
            return issues;
        }
        for (int i = 0; i < intervals.size(); i++) {
            TimeInterval current = intervals.get(i);
            if (!current.hasValidRange()) {
                issues.add(new ScheduleIssue(ScheduleIssue.Code.INVALID_INTERVAL,
                        current.id().isEmpty() ? Integer.toString(i) : current.id()));
            }
            for (int j = i + 1; j < intervals.size(); j++) {
                if (current.overlaps(intervals.get(j))) {
                    issues.add(new ScheduleIssue(ScheduleIssue.Code.OVERLAPPING_INTERVALS,
                            current.id() + "/" + intervals.get(j).id()));
                }
            }
        }
        return issues;
    }

    public static List<ScheduleIssue> validateAll(
            LocalDate date, ZoneId zone, Map<Prayer, Instant> times,
            List<TimeInterval> optional, List<TimeInterval> necessary,
            HijriDate hijri, List<ScheduleIssue> inherited) {
        List<ScheduleIssue> result = new ArrayList<>();
        if (inherited != null) {
            result.addAll(inherited);
        }
        addDistinct(result, validateTimes(date, zone, times));
        addDistinct(result, validateIntervals(optional));
        addDistinct(result, validateIntervals(necessary));
        if ((optional == null || optional.isEmpty())
                && (necessary == null || necessary.isEmpty())) {
            result.add(new ScheduleIssue(ScheduleIssue.Code.INTERVALS_NOT_CONFIGURED, ""));
        }
        if (hijri == null) {
            result.add(new ScheduleIssue(ScheduleIssue.Code.HIJRI_MISSING, ""));
        } else if (!hijri.isPlausible()) {
            result.add(new ScheduleIssue(ScheduleIssue.Code.HIJRI_IMPLAUSIBLE, ""));
        }
        return result;
    }

    private static void addDistinct(List<ScheduleIssue> target, List<ScheduleIssue> items) {
        for (ScheduleIssue item : items) {
            boolean exists = false;
            for (ScheduleIssue existing : target) {
                if (existing.code() == item.code() && existing.detail().equals(item.detail())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                target.add(item);
            }
        }
    }
}
