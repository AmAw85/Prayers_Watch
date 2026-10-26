package com.horloge.islamique.ui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.horloge.islamique.R;
import com.horloge.islamique.core.Correction;
import com.horloge.islamique.core.DateTimeText;
import com.horloge.islamique.core.HijriDate;
import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.SourceInfo;
import com.horloge.islamique.core.TimeEngine;
import com.horloge.islamique.data.AlAdhanClient;
import com.horloge.islamique.data.PrayerDatabase;
import com.horloge.islamique.data.PrayerRepository;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Month view with Gregorian and source-provided Hijri dates and per-day state. */
public final class CalendarActivity extends BaseActivity {

    private PrayerRepository repository;
    private TextView monthTitle;
    private LinearLayout weekHeader;
    private LinearLayout calendarGrid;
    private TextView detailTitle;
    private TextView detailSubtitle;
    private TextView detailStatus;
    private TextView detailSource;
    private LinearLayout detailRows;
    private TextView detailIntervals;

    private LocalDate monthCursor;
    private LocalDate selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        repository = new PrayerRepository(new AlAdhanClient(), new PrayerDatabase(this),
                settings, Clock.systemUTC());
        monthTitle = findViewById(R.id.monthTitle);
        weekHeader = findViewById(R.id.weekHeader);
        calendarGrid = findViewById(R.id.calendarGrid);
        detailTitle = findViewById(R.id.detailTitle);
        detailSubtitle = findViewById(R.id.detailSubtitle);
        detailStatus = findViewById(R.id.detailStatus);
        detailSource = findViewById(R.id.detailSource);
        detailRows = findViewById(R.id.detailRows);
        detailIntervals = findViewById(R.id.detailIntervals);

        LocalDate today = LocalDate.now(repository.zone());
        monthCursor = today.withDayOfMonth(1);
        selectedDate = today;

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        ImageButton previous = findViewById(R.id.previousMonthButton);
        ImageButton next = findViewById(R.id.nextMonthButton);
        Button todayButton = findViewById(R.id.todayButton);
        previous.setOnClickListener(v -> moveMonth(-1));
        next.setOnClickListener(v -> moveMonth(1));
        todayButton.setOnClickListener(v -> {
            LocalDate now = LocalDate.now(repository.zone());
            monthCursor = now.withDayOfMonth(1);
            selectedDate = now;
            render();
        });
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null) {
            render();
        }
    }

    private void moveMonth(int delta) {
        monthCursor = monthCursor.plusMonths(delta);
        selectedDate = monthCursor;
        render();
    }

    private void render() {
        Locale locale = locale();
        monthTitle.setText(DateTimeText.monthTitle(monthCursor, locale));
        renderWeekHeader(locale);

        LocalDate first = monthCursor.withDayOfMonth(1);
        DayOfWeek firstWeekDay = WeekFields.of(locale).getFirstDayOfWeek();
        int offset = (first.getDayOfWeek().getValue()
                - firstWeekDay.getValue() + 7) % 7;
        LocalDate gridStart = first.minusDays(offset);
        LocalDate gridEnd = gridStart.plusDays(41);

        List<PrayerDay> stored = repository.loadRange(gridStart, gridEnd);
        Map<LocalDate, PrayerDay> byDate = new HashMap<>();
        for (PrayerDay day : stored) {
            byDate.put(day.date(), day);
        }

        calendarGrid.removeAllViews();
        LocalDate today = LocalDate.now(repository.zone());
        LocalDate cursor = gridStart;
        for (int rowIndex = 0; rowIndex < 6; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            for (int column = 0; column < 7; column++) {
                LocalDate date = cursor;
                TextView cell = new TextView(this);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(13);
                cell.setLineSpacing(0f, 0.9f);
                cell.setPadding(UiKit.dp(this, 2), UiKit.dp(this, 5),
                        UiKit.dp(this, 2), UiKit.dp(this, 5));
                cell.setMinHeight(UiKit.dp(this, 58));
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                cellParams.setMargins(UiKit.dp(this, 2), UiKit.dp(this, 2),
                        UiKit.dp(this, 2), UiKit.dp(this, 2));

                PrayerDay day = byDate.get(date);
                boolean currentMonth = date.getMonthValue() == monthCursor.getMonthValue()
                        && date.getYear() == monthCursor.getYear();
                boolean selected = date.equals(selectedDate);
                boolean isToday = date.equals(today);

                StringBuilder text = new StringBuilder();
                if (isToday) {
                    text.append("• ");
                }
                text.append(DateTimeText.localizeDigits(
                        Integer.toString(date.getDayOfMonth()), locale));
                if (day != null) {
                    HijriDate hijri = day.hijri();
                    String hijriDay = hijri == null ? "—"
                            : DateTimeText.localizeDigits(
                            Integer.toString(hijri.day()), locale);
                    text.append('\n').append(hijriDay);
                }
                cell.setText(text.toString());
                if (selected) {
                    cell.setBackgroundResource(R.drawable.bg_calendar_selected);
                    cell.setTextColor(getColor(R.color.text_primary));
                    cell.setTypeface(null, android.graphics.Typeface.BOLD);
                } else if (day != null) {
                    cell.setBackgroundResource(R.drawable.bg_card_alt);
                    cell.setTextColor(getColor(R.color.text_primary));
                } else {
                    cell.setBackgroundColor(getColor(R.color.transparent));
                    cell.setTextColor(currentMonth
                            ? getColor(R.color.text_secondary)
                            : getColor(R.color.outline));
                }
                cell.setAlpha(currentMonth ? 1f : 0.65f);

                String contentDescription = DateTimeText.gregorianFull(date, locale);
                if (isToday) {
                    contentDescription += ", " + getString(R.string.calendar_today);
                }
                if (day != null) {
                    contentDescription += ", " + getString(R.string.calendar_source)
                            + ": " + UiKit.providerLabel(this, day.source());
                } else {
                    contentDescription += ", " + getString(R.string.calendar_no_data);
                }
                cell.setContentDescription(contentDescription);
                cell.setClickable(true);
                cell.setOnClickListener(v -> {
                    selectedDate = date;
                    render();
                });
                row.addView(cell, cellParams);
                cursor = cursor.plusDays(1);
            }
            calendarGrid.addView(row);
        }
        renderDetail();
    }

    private void renderWeekHeader(Locale locale) {
        weekHeader.removeAllViews();
        DayOfWeek firstWeekDay = WeekFields.of(locale).getFirstDayOfWeek();
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = firstWeekDay.plus(i);
            TextView label = new TextView(this);
            label.setText(DateTimeText.weekdayShort(day, locale));
            label.setTextColor(getColor(R.color.text_secondary));
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            weekHeader.addView(label);
        }
    }

    private void renderDetail() {
        LocalDate date = selectedDate == null
                ? LocalDate.now(repository.zone()) : selectedDate;
        PrayerDay day = repository.load(date);
        Locale locale = locale();
        detailTitle.setText(DateTimeText.gregorianFull(date, locale));
        if (day != null && day.hijri() != null) {
            detailSubtitle.setText(getString(R.string.hijri_label) + " — "
                    + DateTimeText.hijri(day.hijri(), locale));
        } else {
            detailSubtitle.setText(R.string.hijri_not_available);
        }

        detailRows.removeAllViews();
        if (day == null) {
            detailStatus.setText(getString(R.string.calendar_no_data) + " "
                    + getString(R.string.calendar_select_day));
            detailSource.setText("");
            detailIntervals.setText("");
            detailIntervals.setVisibility(View.GONE);
            return;
        }

        PrayerDay.DataFreshness freshness = day.freshness(Instant.now());
        detailStatus.setText(UiKit.statusSymbol(freshness) + " "
                + getString(UiKit.statusLabel(freshness))
                + (day.corrections().isEmpty() ? ""
                : " · " + getString(R.string.calendar_corrected)));
        SourceInfo source = day.source();
        detailSource.setText(getString(R.string.source_status,
                UiKit.providerLabel(this, source),
                getString(UiKit.sourceKindLabel(source.kind()))));

        Instant sunrise = day.sunrise();
        if (sunrise != null) {
            LinearLayout sunRow = new LinearLayout(this);
            sunRow.setOrientation(LinearLayout.HORIZONTAL);
            sunRow.setGravity(Gravity.CENTER_VERTICAL);
            sunRow.setPadding(0, UiKit.dp(this, 5), 0, UiKit.dp(this, 5));

            sunRow.addView(UiKit.coloredDot(this, getColor(R.color.text_secondary)), UiKit.dotParams(this));
            TextView sunName = new TextView(this);
            sunName.setText(getString(R.string.sunrise));
            sunName.setTextColor(getColor(R.color.text_secondary));
            sunName.setTextSize(14);
            sunName.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            sunRow.addView(sunName);
            TextView sunTime = new TextView(this);
            sunTime.setText(DateTimeText.time(sunrise, day.zone(), locale));
            sunTime.setTextColor(getColor(R.color.text_primary));
            sunTime.setTextSize(14);
            sunTime.setTypeface(null, android.graphics.Typeface.BOLD);
            sunRow.addView(sunTime);
            detailRows.addView(sunRow);
        }

        for (Prayer prayer : Prayer.ALL) {
            Instant instant = day.effectiveTimes().get(prayer);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UiKit.dp(this, 5), 0, UiKit.dp(this, 5));

            row.addView(UiKit.coloredDot(this, colorFor(prayer)), UiKit.dotParams(this));
            TextView name = new TextView(this);
            name.setText(prayerName(prayer));
            name.setTextColor(getColor(R.color.text_primary));
            name.setTextSize(14);
            name.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(name);

            if (day.corrections().containsKey(prayer)) {
                TextView badge = UiKit.badge(this, getString(R.string.corrected_badge),
                        colorFor(prayer));
                LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                badgeParams.setMarginEnd(UiKit.dp(this, 8));
                row.addView(badge, badgeParams);
            }

            TextView time = new TextView(this);
            time.setText(instant == null ? "—"
                    : DateTimeText.time(instant, day.zone(), locale));
            time.setTextColor(getColor(R.color.text_primary));
            time.setTextSize(14);
            time.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(time);
            detailRows.addView(row);
        }

        List<com.horloge.islamique.core.TimeInterval> intervals =
                TimeEngine.prayerIntervals(day, repository.load(date.plusDays(1)));
        if (intervals.isEmpty()) {
            detailIntervals.setText(R.string.calendar_intervals_not_configured);
        } else {
            detailIntervals.setText(getString(R.string.calendar_intervals_configured,
                    intervals.size()));
            for (com.horloge.islamique.core.TimeInterval interval : intervals) {
                addIntervalLine(interval, day.zone(), locale);
            }
        }
        detailIntervals.setVisibility(View.VISIBLE);
    }

    private void addIntervalLine(com.horloge.islamique.core.TimeInterval interval,
                                 ZoneId zone, Locale locale) {
        TextView line = new TextView(this);
        String label = intervalLabel(interval);
        line.setText(label + ": " + DateTimeText.time(interval.start(), zone, locale)
                + " – " + DateTimeText.time(interval.end(), zone, locale));
        line.setTextColor(getColor(R.color.text_secondary));
        line.setTextSize(12);
        line.setPadding(0, UiKit.dp(this, 2), 0, 0);
        detailRows.addView(line);
    }

    private String intervalLabel(com.horloge.islamique.core.TimeInterval interval) {
        Prayer from = Prayer.fromKey(
                interval.id().startsWith("prayer-")
                        ? interval.id().substring("prayer-".length()) : "");
        return from == null ? interval.id() : prayerName(from);
    }

    private String prayerName(Prayer prayer) {
        switch (prayer) {
            case FAJR:
                return getString(R.string.fajr);
            case DHUHR:
                return getString(R.string.dhuhr);
            case ASR:
                return getString(R.string.asr);
            case MAGHRIB:
                return getString(R.string.maghrib);
            case ISHA:
            default:
                return getString(R.string.isha);
        }
    }

    private int colorFor(Prayer prayer) {
        switch (prayer) {
            case FAJR:
                return settings.color(com.horloge.islamique.settings.ColorSlot.FAJR);
            case DHUHR:
                return settings.color(com.horloge.islamique.settings.ColorSlot.DHUHR);
            case ASR:
                return settings.color(com.horloge.islamique.settings.ColorSlot.ASR);
            case MAGHRIB:
                return settings.color(com.horloge.islamique.settings.ColorSlot.MAGHRIB);
            case ISHA:
            default:
                return settings.color(com.horloge.islamique.settings.ColorSlot.ISHA);
        }
    }
}
