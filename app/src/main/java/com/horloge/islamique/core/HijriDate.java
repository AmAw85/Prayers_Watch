package com.horloge.islamique.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Dates of the Hijri calendar exactly as supplied by a data source. */
public final class HijriDate {
    private final int day;
    private final int month;
    private final int year;
    private final String monthNameAr;
    private final String monthNameEn;
    private final List<String> holidays;

    public HijriDate(int day, int month, int year, String monthNameAr,
                     String monthNameEn, List<String> holidays) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.monthNameAr = monthNameAr == null ? "" : monthNameAr;
        this.monthNameEn = monthNameEn == null ? "" : monthNameEn;
        this.holidays = holidays == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(holidays);
    }

    public int day() {
        return day;
    }

    public int month() {
        return month;
    }

    public int year() {
        return year;
    }

    public String monthNameAr() {
        return monthNameAr;
    }

    public String monthNameEn() {
        return monthNameEn;
    }

    public List<String> holidays() {
        return holidays;
    }

    public boolean isPlausible() {
        return day >= 1 && day <= 30 && month >= 1 && month <= 12 && year >= 1300;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HijriDate)) {
            return false;
        }
        HijriDate that = (HijriDate) o;
        return day == that.day && month == that.month && year == that.year
                && monthNameAr.equals(that.monthNameAr)
                && monthNameEn.equals(that.monthNameEn)
                && holidays.equals(that.holidays);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, month, year, monthNameAr, monthNameEn, holidays);
    }
}
