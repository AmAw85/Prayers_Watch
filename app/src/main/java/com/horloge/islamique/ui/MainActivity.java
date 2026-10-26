package com.horloge.islamique.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.horloge.islamique.R;
import com.horloge.islamique.core.DateTimeText;
import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.SourceInfo;
import com.horloge.islamique.core.TimeEngine;
import com.horloge.islamique.core.TimeInterval;
import com.horloge.islamique.data.AlAdhanClient;
import com.horloge.islamique.data.PrayerDatabase;
import com.horloge.islamique.data.PrayerRepository;
import com.horloge.islamique.data.SyncResult;
import com.horloge.islamique.settings.SettingsRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Main screen: clock, next prayer, source state, prayer rows and legend. */
public final class MainActivity extends BaseActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PrayerRepository repository;
    private PrayerClockView clockView;
    private TextView digitalTime;
    private TextView currentState;
    private TextView nextPrayerName;
    private TextView nextPrayerTime;
    private TextView nextPrayerDay;
    private TextView countdownValue;
    private TextView gregorianDate;
    private TextView hijriDate;
    private TextView sourceText;
    private TextView syncDetail;
    private TextView sourceAttribution;
    private TextView statusText;
    private Button statusSyncButton;
    private View statusBar;
    private LinearLayout prayerRows;
    private LinearLayout legendContainer;
    private ImageButton refreshButton;

    private PrayerDay todayDay;
    private PrayerDay yesterdayDay;
    private PrayerDay tomorrowDay;
    private List<PrayerDay> contextDays = Collections.emptyList();
    private LocalDate loadedToday;
    private boolean syncing;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        repository = new PrayerRepository(new AlAdhanClient(), new PrayerDatabase(this),
                settings, Clock.systemUTC());
        bindViews();
        clockView.configure(settings, language(), null, null);
        findViewById(R.id.settingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.calendarButton).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));
        refreshButton.setOnClickListener(v -> syncNow(true));
        statusSyncButton.setOnClickListener(v -> syncNow(true));
        loadFromStore();
        handler.post(ticker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository == null) {
            return;
        }
        loadFromStore();
        if (repository.shouldAutoSync()) {
            syncNow(false);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        clockView = findViewById(R.id.clockView);
        digitalTime = findViewById(R.id.digitalTime);
        currentState = findViewById(R.id.currentState);
        nextPrayerName = findViewById(R.id.nextPrayerName);
        nextPrayerTime = findViewById(R.id.nextPrayerTime);
        nextPrayerDay = findViewById(R.id.nextPrayerDay);
        countdownValue = findViewById(R.id.countdownValue);
        gregorianDate = findViewById(R.id.gregorianDate);
        hijriDate = findViewById(R.id.hijriDate);
        sourceText = findViewById(R.id.sourceText);
        syncDetail = findViewById(R.id.syncDetail);
        sourceAttribution = findViewById(R.id.sourceAttribution);
        statusText = findViewById(R.id.statusText);
        statusSyncButton = findViewById(R.id.statusSyncButton);
        statusBar = findViewById(R.id.statusBar);
        prayerRows = findViewById(R.id.prayerRows);
        legendContainer = findViewById(R.id.legendContainer);
        refreshButton = findViewById(R.id.refreshButton);
    }

    private void tick() {
        if (repository == null) {
            return;
        }
        ZoneId zone = repository.zone();
        LocalDate today = LocalDate.now(zone);
        if (loadedToday == null || !loadedToday.equals(today)) {
            loadFromStore();
            if (repository.shouldAutoSync()) {
                syncNow(false);
            }
            return;
        }
        updateDynamic(Instant.now());
    }

    private void loadFromStore() {
        ZoneId zone = repository.zone();
        LocalDate today = LocalDate.now(zone);
        loadedToday = today;
        yesterdayDay = repository.load(today.minusDays(1));
        todayDay = repository.load(today);
        tomorrowDay = repository.load(today.plusDays(1));
        List<PrayerDay> days = new ArrayList<>();
        if (yesterdayDay != null) {
            days.add(yesterdayDay);
        }
        if (todayDay != null) {
            days.add(todayDay);
        }
        if (tomorrowDay != null) {
            days.add(tomorrowDay);
        }
        contextDays = days;

        clockView.configure(settings, language(), todayDay, tomorrowDay);
        renderPrayerRows();
        renderLegend();
        renderStatic(today);
        updateDynamic(Instant.now());
    }

    private void renderStatic(LocalDate today) {
        Locale locale = locale();
        gregorianDate.setText(DateTimeText.gregorianFull(today, locale));
        if (todayDay != null && todayDay.hijri() != null) {
            hijriDate.setText(getString(R.string.hijri_label) + " — "
                    + DateTimeText.hijri(todayDay.hijri(), locale));
        } else {
            hijriDate.setText(R.string.hijri_not_available);
        }

        if (todayDay != null) {
            SourceInfo source = todayDay.source();
            String provider = UiKit.providerLabel(this, source);
            String kind = getString(UiKit.sourceKindLabel(source.kind()));
            sourceText.setText(getString(R.string.source_status, provider, kind));
            if (source.attribution() == null || source.attribution().isEmpty()) {
                sourceAttribution.setVisibility(View.GONE);
            } else {
                sourceAttribution.setVisibility(View.VISIBLE);
                sourceAttribution.setText(getString(R.string.source_attribution,
                        source.attribution()));
            }
        } else {
            sourceText.setText("");
            sourceAttribution.setVisibility(View.GONE);
        }

        Instant lastSuccess = settings.lastSuccessfulSync();
        if (lastSuccess != null) {
            syncDetail.setText(getString(R.string.status_last_sync,
                    DateTimeText.dateTime(lastSuccess, repository.zone(), locale)));
        } else {
            syncDetail.setText(R.string.status_never);
        }

        renderStatus(today);
    }

    private void renderStatus(LocalDate today) {
        String errorCode = settings.lastSyncError();
        boolean hasError = errorCode != null && !errorCode.isEmpty();
        PrayerDay.DataFreshness freshness;
        StringBuilder text = new StringBuilder();
        String symbol;
        if (todayDay == null) {
            freshness = PrayerDay.DataFreshness.UNAVAILABLE;
            symbol = UiKit.statusSymbol(freshness);
            boolean mismatched = repository.hasMismatchedStoredData(today);
            text.append(symbol).append(' ').append(getString(R.string.no_data_title))
                    .append('\n').append(mismatched
                            ? getString(R.string.status_city_mismatch)
                            : getString(R.string.no_data_body));
            if (hasError) {
                text.append('\n').append(UiKit.errorLabel(this, errorCode));
            }
        } else {
            freshness = todayDay.freshness(Instant.now());
            symbol = UiKit.statusSymbol(freshness);
            text.append(symbol).append(' ').append(getString(UiKit.statusLabel(freshness)));
            Instant lastSuccess = settings.lastSuccessfulSync();
            if (lastSuccess != null) {
                text.append(" · ").append(getString(R.string.status_last_sync,
                        DateTimeText.dateTime(lastSuccess, repository.zone(), locale())));
            }
            if (hasError) {
                text.append('\n').append(UiKit.errorLabel(this, errorCode))
                        .append(' ').append(getString(R.string.sync_error_kept));
            }
            if (freshness == PrayerDay.DataFreshness.STALE) {
                text.append('\n').append(getString(R.string.status_offline));
            }
        }
        if (syncing) {
            text.setLength(0);
            text.append(getString(R.string.syncing));
            freshness = PrayerDay.DataFreshness.STALE;
        }
        statusText.setText(text.toString());
        Drawable background = getDrawable(R.drawable.bg_status).mutate();
        background.setTint(UiKit.statusBackground(this, freshness));
        statusBar.setBackground(background);
        boolean canSync = !SettingsRepository.PROVIDER_NONE.equals(settings.providerId())
                && !syncing;
        UiKit.setVisible(statusSyncButton, canSync && (
                todayDay == null
                        || hasError
                        || todayDay.freshness(Instant.now())
                        != PrayerDay.DataFreshness.CURRENT));
        refreshButton.setEnabled(!syncing);
        refreshButton.setAlpha(syncing ? 0.4f : 1f);
        statusSyncButton.setEnabled(!syncing);
    }

    private void renderPrayerRows() {
        prayerRows.removeAllViews();
        if (todayDay == null) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setBackgroundResource(R.drawable.bg_card);
            empty.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 16),
                    UiKit.dp(this, 16), UiKit.dp(this, 16));
            TextView title = new TextView(this);
            title.setText(R.string.no_data_title);
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(16);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            empty.addView(title);
            TextView body = new TextView(this);
            body.setText(R.string.no_data_body);
            body.setTextColor(getColor(R.color.text_secondary));
            body.setTextSize(13);
            body.setPadding(0, UiKit.dp(this, 4), 0, UiKit.dp(this, 10));
            empty.addView(body);
            Button sync = new Button(this, null, 0, R.style.HorlogeButton);
            sync.setText(R.string.sync_now);
            sync.setAllCaps(false);
            sync.setOnClickListener(v -> syncNow(true));
            empty.addView(sync, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            prayerRows.addView(empty);
            return;
        }
        for (Prayer prayer : Prayer.ALL) {
            Instant source = todayDay.originalTimes().get(prayer);
            Instant effective = todayDay.effectiveTimes().get(prayer);
            boolean corrected = todayDay.corrections().containsKey(prayer);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_card);
            row.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 10),
                    UiKit.dp(this, 8), UiKit.dp(this, 10));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = UiKit.dp(this, 6);

            View dot = UiKit.coloredDot(this, colorFor(prayer));
            row.addView(dot, UiKit.dotParams(this));

            LinearLayout nameBlock = new LinearLayout(this);
            nameBlock.setOrientation(LinearLayout.VERTICAL);
            nameBlock.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView name = new TextView(this);
            name.setText(prayerName(prayer));
            name.setTextColor(getColor(R.color.text_primary));
            name.setTextSize(16);
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            nameBlock.addView(name);
            if (corrected) {
                LinearLayout badgeRow = new LinearLayout(this);
                badgeRow.setOrientation(LinearLayout.HORIZONTAL);
                TextView badge = UiKit.badge(this, getString(R.string.corrected_badge),
                        colorFor(prayer));
                badgeRow.addView(badge);
                nameBlock.addView(badgeRow);
            }
            row.addView(nameBlock);

            TextView time = new TextView(this);
            time.setText(effective == null ? "—" : DateTimeText.time(effective,
                    repository.zone(), locale()));
            time.setTextColor(corrected ? colorFor(prayer) : getColor(R.color.text_primary));
            time.setTextSize(17);
            time.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(time);

            ImageButton edit = new ImageButton(this);
            edit.setBackgroundResource(R.drawable.bg_dot);
            Drawable editBg = edit.getBackground().mutate();
            editBg.setTint(getColor(R.color.surface_alt));
            edit.setBackground(editBg);
            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                    UiKit.dp(this, 38), UiKit.dp(this, 38));
            editParams.setMarginStart(UiKit.dp(this, 10));
            edit.setLayoutParams(editParams);
            edit.setPadding(UiKit.dp(this, 9), UiKit.dp(this, 9),
                    UiKit.dp(this, 9), UiKit.dp(this, 9));
            edit.setImageResource(R.drawable.ic_edit);
            edit.setImageTintList(UiKit.tint(getColor(R.color.text_primary)));
            edit.setContentDescription(getString(R.string.edit_time_for, prayerName(prayer)));
            edit.setEnabled(source != null);
            edit.setOnClickListener(v -> CorrectionDialog.show(this, repository,
                    repository.load(todayDay.date()), prayer, () -> {
                        loadFromStore();
                        renderStatus(todayDay == null ? LocalDate.now(repository.zone())
                                : todayDay.date());
                    }));
            row.addView(edit);

            row.setOnClickListener(v -> CorrectionDialog.show(this, repository,
                    repository.load(todayDay.date()), prayer, this::loadFromStore));
            prayerRows.addView(row, rowParams);
        }
    }

    private void renderLegend() {
        legendContainer.removeAllViews();
        addRingHeader(1, R.string.ring_prayer_title, R.string.ring_prayer_desc, null);

        for (Prayer prayer : Prayer.ALL) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(UiKit.dp(this, 4), UiKit.dp(this, 3),
                    UiKit.dp(this, 4), UiKit.dp(this, 3));
            line.addView(UiKit.coloredDot(this, colorFor(prayer)), UiKit.dotParams(this));
            TextView label = new TextView(this);
            label.setText(prayerName(prayer));
            label.setTextColor(getColor(R.color.text_primary));
            label.setTextSize(13);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            line.addView(label);
            TextView time = new TextView(this);
            Instant effective = todayDay == null ? null : todayDay.effectiveTimes().get(prayer);
            time.setText(effective == null ? "—" : DateTimeText.time(effective,
                    repository.zone(), locale()));
            time.setTextColor(getColor(R.color.text_secondary));
            time.setTextSize(13);
            line.addView(time);
            legendContainer.addView(line);
        }

        addRingHeader(2, R.string.ring_optional_title, R.string.ring_optional_desc,
                settings.color(com.horloge.islamique.settings.ColorSlot.OPTIONAL));
        addIntervalsLegend(todayDay == null ? Collections.emptyList()
                : todayDay.optionalIntervals(), R.string.prayer_optional,
                R.string.ring_not_configured);

        addRingHeader(3, R.string.ring_necessary_title, R.string.ring_necessary_desc,
                settings.color(com.horloge.islamique.settings.ColorSlot.NECESSARY));
        addIntervalsLegend(todayDay == null ? Collections.emptyList()
                : todayDay.necessaryIntervals(), R.string.prayer_necessary,
                R.string.ring_not_configured);
    }

    private void addRingHeader(int number, int titleRes, int descRes, Integer color) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.TOP);
        root.setPadding(0, UiKit.dp(this, 12), 0, UiKit.dp(this, 2));

        TextView numberView = new TextView(this);
        numberView.setText(DateTimeText.localizeDigits(Integer.toString(number), locale()));
        numberView.setTextColor(getColor(R.color.text_primary));
        numberView.setTextSize(12);
        numberView.setGravity(Gravity.CENTER);
        numberView.setBackgroundResource(R.drawable.bg_badge);
        numberView.setWidth(UiKit.dp(this, 24));
        numberView.setHeight(UiKit.dp(this, 24));
        root.addView(numberView);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        blockParams.setMarginStart(UiKit.dp(this, 8));
        TextView title = new TextView(this);
        title.setText(titleRes);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        textBlock.addView(title);
        TextView desc = new TextView(this);
        desc.setText(descRes);
        desc.setTextColor(getColor(R.color.text_secondary));
        desc.setTextSize(12);
        textBlock.addView(desc);
        root.addView(textBlock, blockParams);

        if (color != null) {
            View dot = UiKit.coloredDot(this, color);
            LinearLayout.LayoutParams dotParams = UiKit.dotParams(this);
            dotParams.setMarginStart(UiKit.dp(this, 6));
            root.addView(dot, dotParams);
        }
        legendContainer.addView(root);
    }

    private void addIntervalsLegend(List<TimeInterval> intervals, int fallbackLabel,
                                    int notConfiguredRes) {
        if (intervals == null || intervals.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(notConfiguredRes);
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(12);
            empty.setPadding(UiKit.dp(this, 4), UiKit.dp(this, 2),
                    UiKit.dp(this, 4), UiKit.dp(this, 6));
            legendContainer.addView(empty);
            return;
        }
        for (TimeInterval interval : intervals) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.VERTICAL);
            line.setPadding(UiKit.dp(this, 4), UiKit.dp(this, 3),
                    UiKit.dp(this, 4), UiKit.dp(this, 3));
            TextView title = new TextView(this);
            title.setText(interval.label().isEmpty()
                    ? getString(fallbackLabel) : interval.label());
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(13);
            line.addView(title);
            TextView range = new TextView(this);
            range.setText(formatInterval(interval));
            range.setTextColor(getColor(R.color.text_secondary));
            range.setTextSize(12);
            line.addView(range);
            if (!interval.provenance().isEmpty()) {
                TextView provenance = new TextView(this);
                provenance.setText(interval.provenance());
                provenance.setTextColor(getColor(R.color.text_secondary));
                provenance.setTextSize(11);
                line.addView(provenance);
            }
            legendContainer.addView(line);
        }
    }

    private String formatInterval(TimeInterval interval) {
        ZoneId zone = repository.zone();
        Locale locale = locale();
        String start;
        String end;
        LocalDate startDate = interval.start().atZone(zone).toLocalDate();
        LocalDate endDate = interval.end().atZone(zone).toLocalDate();
        if (startDate.equals(endDate)) {
            start = DateTimeText.time(interval.start(), zone, locale);
            end = DateTimeText.time(interval.end(), zone, locale);
        } else {
            start = DateTimeText.dateTime(interval.start(), zone, locale);
            end = DateTimeText.dateTime(interval.end(), zone, locale);
        }
        return start + " – " + end;
    }

    private void updateDynamic(Instant now) {
        if (repository == null) {
            return;
        }
        ZoneId zone = repository.zone();
        digitalTime.setText(DateTimeText.timeWithSeconds(now, zone, locale()));
        clockView.setNow(now);

        Optional<TimeEngine.CurrentPrayer> current =
                TimeEngine.currentPrayer(now, contextDays);
        PrayerDay activeDay = null;
        if (current.isPresent()) {
            for (PrayerDay day : contextDays) {
                if (day.date().equals(current.get().day)) {
                    activeDay = day;
                    break;
                }
            }
            clockView.setActivePrayer(current.get().prayer, current.get().day);
            String prayerName = prayerName(current.get().prayer);
            List<String> activeLabels = new ArrayList<>();
            if (activeDay != null) {
                for (TimeInterval interval : activeDay.optionalIntervals()) {
                    if (interval.contains(now)) {
                        activeLabels.add(interval.label().isEmpty()
                                ? getString(R.string.prayer_optional) : interval.label());
                    }
                }
                for (TimeInterval interval : activeDay.necessaryIntervals()) {
                    if (interval.contains(now)) {
                        activeLabels.add(interval.label().isEmpty()
                                ? getString(R.string.prayer_necessary) : interval.label());
                    }
                }
            }
            StringBuilder state = new StringBuilder();
            state.append(getString(R.string.current_prayer)).append(": ").append(prayerName);
            if (!activeLabels.isEmpty()) {
                state.append(" · ").append(getString(R.string.now_active,
                        join(activeLabels)));
            } else if (activeDay != null
                    && activeDay.optionalIntervals().isEmpty()
                    && activeDay.necessaryIntervals().isEmpty()) {
                state.append(" · ").append(getString(R.string.intervals_not_configured));
            } else if (activeDay != null) {
                state.append(" · ").append(getString(R.string.no_active_interval));
            }
            currentState.setText(state.toString());
        } else {
            clockView.setActivePrayer(null, null);
            currentState.setText(R.string.no_current_prayer);
        }

        Optional<TimeEngine.NextPrayer> next = TimeEngine.nextPrayer(now, contextDays);
        if (next.isPresent()) {
            TimeEngine.NextPrayer prayer = next.get();
            nextPrayerName.setText(prayerName(prayer.prayer));
            nextPrayerTime.setText(getString(R.string.next_prayer_at,
                    DateTimeText.time(prayer.instant, zone, locale())));
            LocalDate today = LocalDate.now(zone);
            if (prayer.day.equals(today.plusDays(1))) {
                nextPrayerDay.setText(getString(R.string.date_tomorrow));
            } else if (!prayer.day.equals(today)) {
                nextPrayerDay.setText(DateTimeText.gregorianMedium(prayer.day, locale()));
            } else {
                nextPrayerDay.setText("");
            }
            countdownValue.setText(DateTimeText.countdown(
                    TimeEngine.secondsUntil(now, prayer.instant), locale()));
        } else {
            nextPrayerName.setText(R.string.no_next_prayer);
            nextPrayerTime.setText("");
            nextPrayerDay.setText("");
            countdownValue.setText("—");
        }
    }

    private void syncNow(boolean userInitiated) {
        if (syncing) {
            return;
        }
        if (SettingsRepository.PROVIDER_NONE.equals(settings.providerId())) {
            Toast.makeText(this, R.string.error_no_provider, Toast.LENGTH_SHORT).show();
            return;
        }
        syncing = true;
        renderStatus(loadedToday == null ? LocalDate.now(repository.zone()) : loadedToday);
        executor.execute(() -> {
            ZoneId zone = repository.zone();
            LocalDate today = LocalDate.now(zone);
            SyncResult result = repository.sync(today.minusDays(1), 3);
            handler.post(() -> {
                syncing = false;
                loadFromStore();
                if (userInitiated) {
                    if (result.success) {
                        Toast.makeText(MainActivity.this,
                                getString(R.string.sync_success, result.savedDays),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this,
                                getString(R.string.sync_error,
                                        UiKit.errorLabel(MainActivity.this, result.errorCode)),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
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

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
