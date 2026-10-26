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

    public static List<ScheduleIssue> validateAll(
            LocalDate date, ZoneId zone, Map<Prayer, Instant> times,
            HijriDate hijri, List<ScheduleIssue> inherited) {
        List<ScheduleIssue> result = new ArrayList<>();
        if (inherited != null) {
            result.addAll(inherited);
        }
        addDistinct(result, validateTimes(date, zone, times));
        if (hijri == null) {
            result.add(new ScheduleIssue(ScheduleIssue.Code.HIJRI_MISSING, ""));
        } else if (!hijri.isPlausible()) {
            result.add(new ScheduleIssue(ScheduleIssue.Code.HIJRI_IMPLAUSIBLE, hijriPlausibleDetail(hijri)));
        }
        return result;
    }

    private static String hijriPlausibleDetail(HijriDate hijri) {
        return hijri.day() + "-" + hijri.month() + "-" + hijri.year();
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
