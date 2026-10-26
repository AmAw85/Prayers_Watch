package com.horloge.islamique.core;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Pure temporal logic, independent from Android and from network providers. */
public final class TimeEngine {

    private TimeEngine() {
    }

    public static final class CurrentPrayer {
        public final Prayer prayer;
        public final LocalDate day;
        public final Instant start;
        public final Instant end;

        CurrentPrayer(Prayer prayer, LocalDate day, Instant start, Instant end) {
            this.prayer = prayer;
            this.day = day;
            this.start = start;
            this.end = end;
        }
    }

    public static final class NextPrayer {
        public final Prayer prayer;
        public final LocalDate day;
        public final Instant instant;

        NextPrayer(Prayer prayer, LocalDate day, Instant instant) {
            this.prayer = prayer;
            this.day = day;
            this.instant = instant;
        }

        public Duration countdown(Instant now) {
            Duration duration = Duration.between(now, instant);
            return duration.isNegative() ? Duration.ZERO : duration;
        }
    }

    public static Optional<CurrentPrayer> currentPrayer(Instant now, List<PrayerDay> days) {
        List<Slot> slots = sortedSlots(days);
        Slot current = null;
        Slot next = null;
        for (Slot slot : slots) {
            if (slot.instant.isAfter(now)) {
                next = slot;
                break;
            }
            current = slot;
        }
        if (current == null) {
            return Optional.empty();
        }
        Instant end = next == null ? current.instant.plus(Duration.ofDays(1)) : next.instant;
        return Optional.of(new CurrentPrayer(current.prayer, current.day, current.instant, end));
    }

    public static Optional<NextPrayer> nextPrayer(Instant now, List<PrayerDay> days) {
        Slot best = null;
        for (Slot slot : sortedSlots(days)) {
            if (slot.instant.isAfter(now)
                    && (best == null || slot.instant.isBefore(best.instant))) {
                best = slot;
            }
        }
        return best == null
                ? Optional.empty()
                : Optional.of(new NextPrayer(best.prayer, best.day, best.instant));
    }

    /**
     * Sectors used only to identify prayers on the clock. They are NOT claims about
     * the valid religious time; that information requires explicit, sourced intervals.
     */
    public static List<TimeInterval> prayerSectors(PrayerDay day, PrayerDay nextDay) {
        List<TimeInterval> result = new ArrayList<>();
        Map<Prayer, Instant> times = day.effectiveTimes();
        for (int i = 0; i < Prayer.ALL.size(); i++) {
            Prayer prayer = Prayer.ALL.get(i);
            Instant start = times.get(prayer);
            if (start == null) {
                continue;
            }
            Instant end = null;
            for (int j = i + 1; j < Prayer.ALL.size(); j++) {
                end = times.get(Prayer.ALL.get(j));
                if (end != null) {
                    break;
                }
            }
            if (end == null) {
                if (nextDay != null && nextDay.effectiveTimes().get(Prayer.FAJR) != null) {
                    end = nextDay.effectiveTimes().get(Prayer.FAJR);
                } else {
                    end = day.date().plusDays(1).atStartOfDay(day.zone()).toInstant();
                }
            }
            if (end.isAfter(start)) {
                result.add(new TimeInterval("prayer-" + prayer.key(), "", start, end,
                        day.source().displayName()));
            }
        }
        return result;
    }

    public static List<TimeInterval> activeIntervals(Instant now, List<TimeInterval> intervals) {
        List<TimeInterval> active = new ArrayList<>();
        for (TimeInterval interval : intervals) {
            if (interval.contains(now)) {
                active.add(interval);
            }
        }
        return active;
    }

    public static long secondsUntil(Instant now, Instant target) {
        long seconds = Duration.between(now, target).getSeconds();
        return Math.max(0L, seconds);
    }

    public static LocalDate dateAt(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }

    public static Instant startOfDay(LocalDate date, ZoneId zone) {
        return date.atStartOfDay(zone).toInstant();
    }

    public static Optional<ScheduleIssue> localTimeProblem(
            LocalDate date, java.time.LocalTime time, ZoneId zone) {
        java.time.LocalDateTime local = java.time.LocalDateTime.of(date, time);
        ZoneRules rules = zone.getRules();
        List<ZoneOffset> offsets = rules.getValidOffsets(local);
        if (offsets.isEmpty()) {
            ZoneOffsetTransition transition = rules.getTransition(local);
            return Optional.of(new ScheduleIssue(ScheduleIssue.Code.NONEXISTENT_LOCAL_TIME,
                    transition == null ? local.toString() : transition.toString()));
        }
        if (offsets.size() > 1) {
            return Optional.of(new ScheduleIssue(ScheduleIssue.Code.AMBIGUOUS_LOCAL_TIME,
                    local + " " + offsets));
        }
        return Optional.empty();
    }

    private static List<Slot> sortedSlots(List<PrayerDay> days) {
        List<Slot> slots = new ArrayList<>();
        if (days == null) {
            return slots;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (PrayerDay day : days) {
            if (day == null) {
                continue;
            }
            Map<Prayer, Instant> times = new EnumMap<>(Prayer.class);
            times.putAll(day.effectiveTimes());
            for (Prayer prayer : Prayer.ALL) {
                Instant instant = times.get(prayer);
                if (instant == null) {
                    continue;
                }
                String key = day.date() + "/" + prayer.name() + "/" + instant;
                if (seen.add(key)) {
                    slots.add(new Slot(prayer, day.date(), instant));
                }
            }
        }
        slots.sort(Comparator.comparing(slot -> slot.instant));
        return Collections.unmodifiableList(slots);
    }

    private static final class Slot {
        final Prayer prayer;
        final LocalDate day;
        final Instant instant;

        Slot(Prayer prayer, LocalDate day, Instant instant) {
            this.prayer = prayer;
            this.day = day;
            this.instant = instant;
        }
    }
}
