package com.horloge.islamique.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.horloge.islamique.R;
import com.horloge.islamique.core.Correction;
import com.horloge.islamique.core.DateTimeText;
import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.data.AlAdhanClient;
import com.horloge.islamique.data.City;
import com.horloge.islamique.data.PrayerDatabase;
import com.horloge.islamique.data.PrayerRepository;
import com.horloge.islamique.data.SyncResult;
import com.horloge.islamique.settings.AppLanguage;
import com.horloge.islamique.settings.ColorSlot;
import com.horloge.islamique.settings.SettingsRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/** Language, colours, source, corrections and import/about information. */
public final class SettingsActivity extends BaseActivity {

    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PrayerRepository repository;
    private Spinner languageSpinner;
    private Spinner providerSpinner;
    private Spinner citySpinner;
    private Spinner correctionPrayerSpinner;
    private LinearLayout colorsContainer;
    private LinearLayout correctionsContainer;
    private EditText timezoneInput;
    private TextView timezoneError;
    private TextView lastSyncValue;
    private TextView lastAttemptValue;
    private TextView validityValue;
    private TextView dataErrorValue;
    private TextView correctionOriginalValue;
    private TextView correctionError;
    private Button correctionDateButton;
    private Button correctionTimeButton;
    private Button syncButton;
    private EditText correctionReasonInput;
    private EditText correctionProvenanceInput;

    private LocalDate correctionDate;
    private Instant correctionTime;
    private PrayerDay selectedCorrectionDay;
    private boolean binding;
    private boolean syncing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        repository = new PrayerRepository(new AlAdhanClient(), new PrayerDatabase(this),
                settings, Clock.systemUTC());
        correctionDate = LocalDate.now(repository.zone());
        bindViews();
        setupListeners();
        setupSpinners();
        renderColors();
        loadState();
        loadCorrectionDay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository == null) {
            return;
        }
        if (correctionDate == null) {
            correctionDate = LocalDate.now(repository.zone());
        }
        renderColors();
        loadState();
        loadCorrectionDay();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        ImageButton back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());
        languageSpinner = findViewById(R.id.languageSpinner);
        providerSpinner = findViewById(R.id.providerSpinner);
        citySpinner = findViewById(R.id.citySpinner);
        correctionPrayerSpinner = findViewById(R.id.correctionPrayerSpinner);
        colorsContainer = findViewById(R.id.colorsContainer);
        correctionsContainer = findViewById(R.id.correctionsContainer);
        timezoneInput = findViewById(R.id.timezoneInput);
        timezoneError = findViewById(R.id.timezoneError);
        lastSyncValue = findViewById(R.id.lastSyncValue);
        lastAttemptValue = findViewById(R.id.lastAttemptValue);
        validityValue = findViewById(R.id.validityValue);
        dataErrorValue = findViewById(R.id.dataErrorValue);
        correctionOriginalValue = findViewById(R.id.correctionOriginalValue);
        correctionError = findViewById(R.id.correctionError);
        correctionDateButton = findViewById(R.id.correctionDateButton);
        correctionTimeButton = findViewById(R.id.correctionTimeButton);
        syncButton = findViewById(R.id.syncButton);
        correctionReasonInput = findViewById(R.id.correctionReasonInput);
        correctionProvenanceInput = findViewById(R.id.correctionProvenanceInput);
        findViewById(R.id.resetColorsButton).setOnClickListener(v -> confirmResetColors());
        findViewById(R.id.applyTimezoneButton).setOnClickListener(v -> applyTimezone());
        findViewById(R.id.saveCorrectionButton).setOnClickListener(v -> saveCorrection());
    }

    private void setupListeners() {
        syncButton.setOnClickListener(v -> syncNow());
        correctionDateButton.setOnClickListener(v -> pickCorrectionDate());
        correctionTimeButton.setOnClickListener(v -> pickCorrectionTime());
    }

    private void setupSpinners() {
        binding = true;

        List<String> languages = new ArrayList<>();
        for (AppLanguage language : AppLanguage.values()) {
            languages.add(language.nativeName());
        }
        languageSpinner.setAdapter(new TintedSpinnerAdapter(this, languages));
        languageSpinner.setSelection(settings.language().ordinal());

        List<String> providers = new ArrayList<>();
        providers.add(getString(R.string.provider_aladhan));
        providers.add(getString(R.string.provider_none));
        providerSpinner.setAdapter(new TintedSpinnerAdapter(this, providers));
        providerSpinner.setSelection(SettingsRepository.PROVIDER_NONE
                .equals(settings.providerId()) ? 1 : 0);

        List<String> cities = new ArrayList<>();
        int cityIndex = 0;
        for (int i = 0; i < City.ALGERIA.size(); i++) {
            City city = City.ALGERIA.get(i);
            cities.add(city.name(language().code()));
            if (city.id().equals(settings.cityId())) {
                cityIndex = i;
            }
        }
        citySpinner.setAdapter(new TintedSpinnerAdapter(this, cities));
        citySpinner.setSelection(cityIndex);
        timezoneInput.setText(settings.timezoneId());

        List<String> prayers = new ArrayList<>();
        for (Prayer prayer : Prayer.ALL) {
            prayers.add(prayerName(prayer));
        }
        correctionPrayerSpinner.setAdapter(new TintedSpinnerAdapter(this, prayers));
        correctionPrayerSpinner.setSelection(0);

        binding = false;

        languageSpinner.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(int position) {
                if (binding) {
                    return;
                }
                AppLanguage selected = AppLanguage.values()[position];
                if (selected != settings.language()) {
                    settings.setLanguage(selected);
                    recreate();
                }
            }
        });
        providerSpinner.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(int position) {
                if (binding) {
                    return;
                }
                settings.setProviderId(position == 1
                        ? SettingsRepository.PROVIDER_NONE
                        : SettingsRepository.PROVIDER_ALADHAN);
                loadState();
            }
        });
        citySpinner.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(int position) {
                if (binding) {
                    return;
                }
                City city = City.ALGERIA.get(position);
                if (!city.id().equals(settings.cityId())) {
                    settings.setCityId(city.id());
                    settings.setTimezoneId(city.zoneId());
                    timezoneInput.setText(city.zoneId());
                    Toast.makeText(SettingsActivity.this, R.string.city_changed,
                            Toast.LENGTH_LONG).show();
                    loadState();
                    loadCorrectionDay();
                }
            }
        });
        correctionPrayerSpinner.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(int position) {
                if (!binding) {
                    loadCorrectionDay();
                }
            }
        });

        timezoneInput.setOnEditorActionListener((v, actionId, event) -> {
            applyTimezone();
            return true;
        });
    }

    private void applyTimezone() {
        String value = timezoneInput.getText().toString().trim();
        try {
            ZoneId zone = ZoneId.of(value);
            String previous = settings.timezoneId();
            settings.setTimezoneId(zone.getId());
            timezoneInput.setText(zone.getId());
            timezoneError.setVisibility(View.GONE);
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            if (!previous.equals(zone.getId())) {
                loadState();
                loadCorrectionDay();
            }
        } catch (RuntimeException ex) {
            timezoneError.setText(R.string.timezone_invalid);
            timezoneError.setVisibility(View.VISIBLE);
        }
    }

    private void renderColors() {
        colorsContainer.removeAllViews();
        for (ColorSlot slot : ColorSlot.values()) {
            int color = settings.color(slot);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_card);
            row.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 8),
                    UiKit.dp(this, 8), UiKit.dp(this, 8));
            row.setClickable(true);
            row.setContentDescription(getString(R.string.color_edit_for, colorLabel(slot)));
            row.setOnClickListener(v -> showColorDialog(slot));

            View dot = UiKit.coloredDot(this, color);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    UiKit.dp(this, 26), UiKit.dp(this, 26));
            dotParams.setMarginEnd(UiKit.dp(this, 12));
            row.addView(dot, dotParams);

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView name = new TextView(this);
            name.setText(colorLabel(slot));
            name.setTextColor(getColor(R.color.text_primary));
            name.setTextSize(14);
            labels.addView(name);
            TextView hex = new TextView(this);
            hex.setText(settings.colorHex(slot));
            hex.setTextColor(getColor(R.color.text_secondary));
            hex.setTextSize(12);
            labels.addView(hex);
            row.addView(labels);

            ImageButton edit = new ImageButton(this);
            edit.setBackgroundResource(R.drawable.bg_dot);
            Drawable bg = edit.getBackground().mutate();
            bg.setTint(getColor(R.color.surface_alt));
            edit.setBackground(bg);
            edit.setPadding(UiKit.dp(this, 8), UiKit.dp(this, 8),
                    UiKit.dp(this, 8), UiKit.dp(this, 8));
            edit.setImageResource(R.drawable.ic_edit);
            edit.setImageTintList(UiKit.tint(getColor(R.color.text_primary)));
            edit.setContentDescription(getString(R.string.color_edit_for, colorLabel(slot)));
            edit.setOnClickListener(v -> showColorDialog(slot));
            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                    UiKit.dp(this, 38), UiKit.dp(this, 38));
            row.addView(edit, editParams);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = UiKit.dp(this, 6);
            colorsContainer.addView(row, rowParams);
        }
    }

    private void showColorDialog(ColorSlot slot) {
        int initial = settings.color(slot);
        int[] rgb = {(initial >> 16) & 0xFF, (initial >> 8) & 0xFF, initial & 0xFF};

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = UiKit.dp(this, 18);
        root.setPadding(padding, padding, padding, UiKit.dp(this, 4));

        View preview = new View(this);
        preview.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 56));
        root.addView(preview, previewParams);

        TextView hexLabel = new TextView(this);
        hexLabel.setText(R.string.color_hex);
        hexLabel.setTextColor(getColor(R.color.text_secondary));
        hexLabel.setTextSize(12);
        hexLabel.setPadding(0, UiKit.dp(this, 10), 0, UiKit.dp(this, 2));
        root.addView(hexLabel);

        EditText hexInput = new EditText(this);
        hexInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        hexInput.setText(String.format(Locale.ROOT, "#%02X%02X%02X", rgb[0], rgb[1], rgb[2]));
        hexInput.setTextSize(14);
        hexInput.setTextColor(getColor(R.color.text_primary));
        hexInput.setBackgroundResource(R.drawable.bg_input);
        hexInput.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 8),
                UiKit.dp(this, 12), UiKit.dp(this, 8));
        root.addView(hexInput);

        TextView error = new TextView(this);
        error.setTextColor(getColor(R.color.danger));
        error.setTextSize(12);
        error.setVisibility(View.GONE);
        root.addView(error);

        TextView warning = new TextView(this);
        warning.setText(R.string.color_low_contrast);
        warning.setTextColor(getColor(R.color.warning));
        warning.setTextSize(12);
        warning.setVisibility(View.GONE);
        root.addView(warning);

        SeekBar[] bars = new SeekBar[3];
        int[] labels = {R.string.color_red, R.string.color_green, R.string.color_blue};
        for (int i = 0; i < 3; i++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(0, UiKit.dp(this, 6), 0, 0);
            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setTextColor(getColor(R.color.text_secondary));
            label.setTextSize(12);
            label.setWidth(UiKit.dp(this, 46));
            line.addView(label);
            SeekBar bar = new SeekBar(this);
            bar.setMax(255);
            bar.setProgress(rgb[i]);
            bar.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            line.addView(bar);
            TextView value = new TextView(this);
            value.setText(DateTimeText.localizeDigits(Integer.toString(rgb[i]), locale()));
            value.setTextColor(getColor(R.color.text_primary));
            value.setTextSize(12);
            value.setWidth(UiKit.dp(this, 38));
            value.setGravity(Gravity.END);
            line.addView(value);
            root.addView(line);
            bars[i] = bar;
        }

        boolean[] updating = {false};
        Runnable applyPreview = () -> {
            int color = Color.rgb(rgb[0], rgb[1], rgb[2]);
            preview.setBackgroundResource(R.drawable.bg_card);
            Drawable bg = preview.getBackground().mutate();
            bg.setTint(color);
            preview.setBackground(bg);
            if (!updating[0]) {
                hexInput.setText(String.format(Locale.ROOT, "#%02X%02X%02X", rgb[0], rgb[1], rgb[2]));
            }
            for (int i = 0; i < 3; i++) {
                TextView value = (TextView) ((LinearLayout) bars[i].getParent()).getChildAt(2);
                value.setText(DateTimeText.localizeDigits(Integer.toString(rgb[i]), locale()));
            }
            double contrast = UiKit.contrastRatio(color, getColor(R.color.clock_face));
            warning.setVisibility(contrast < 3.0 ? View.VISIBLE : View.GONE);
        };
        applyPreview.run();

        for (int i = 0; i < 3; i++) {
            final int channel = i;
            bars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) {
                        return;
                    }
                    rgb[channel] = progress;
                    updating[0] = false;
                    applyPreview.run();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String raw = s.toString().trim();
                if (!HEX.matcher(raw).matches()) {
                    if (raw.length() == 7) {
                        error.setText(R.string.color_invalid);
                        error.setVisibility(View.VISIBLE);
                    }
                    return;
                }
                int parsed;
                try {
                    parsed = Color.parseColor(raw);
                } catch (RuntimeException ex) {
                    return;
                }
                error.setVisibility(View.GONE);
                updating[0] = true;
                rgb[0] = (parsed >> 16) & 0xFF;
                rgb[1] = (parsed >> 8) & 0xFF;
                rgb[2] = parsed & 0xFF;
                for (int i = 0; i < 3; i++) {
                    bars[i].setProgress(rgb[i]);
                }
                applyPreview.run();
                updating[0] = false;
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.color_edit_for, colorLabel(slot)))
                .setView(root)
                .setPositiveButton(R.string.color_apply, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String raw = hexInput.getText().toString().trim();
                    if (!HEX.matcher(raw).matches()) {
                        error.setText(R.string.color_invalid);
                        error.setVisibility(View.VISIBLE);
                        return;
                    }
                    settings.setColor(slot, Color.parseColor(raw));
                    Toast.makeText(this, R.string.color_saved, Toast.LENGTH_SHORT).show();
                    renderColors();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void confirmResetColors() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.reset_colors_confirm)
                .setPositiveButton(R.string.reset_colors, (dialog, which) -> {
                    settings.resetColors();
                    renderColors();
                    Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadState() {
        timezoneInput.setText(settings.timezoneId());
        Instant lastSuccess = settings.lastSuccessfulSync();
        lastSyncValue.setText(lastSuccess == null
                ? getString(R.string.status_never)
                : getString(R.string.status_last_sync,
                DateTimeText.dateTime(lastSuccess, repository.zone(), locale())));
        Instant lastAttempt = settings.lastSyncAttempt();
        if (lastAttempt == null) {
            lastAttemptValue.setText(getString(R.string.last_attempt_label) + ": "
                    + getString(R.string.never));
        } else {
            lastAttemptValue.setText(getString(R.string.last_attempt_label) + ": "
                    + DateTimeText.dateTime(lastAttempt, repository.zone(), locale()));
        }

        PrayerDay today = repository.load(LocalDate.now(repository.zone()));
        if (today == null) {
            boolean mismatched = repository.hasMismatchedStoredData(
                    LocalDate.now(repository.zone()));
            validityValue.setText(getString(R.string.validity_label) + ": "
                    + getString(mismatched
                    ? R.string.status_city_mismatch : R.string.status_unavailable));
        } else {
            PrayerDay.DataFreshness freshness = today.freshness(Instant.now());
            validityValue.setText(getString(R.string.validity_label) + ": "
                    + UiKit.statusSymbol(freshness) + " "
                    + getString(UiKit.statusLabel(freshness)));
        }

        String errorCode = settings.lastSyncError();
        if (errorCode == null || errorCode.isEmpty()) {
            dataErrorValue.setVisibility(View.GONE);
        } else {
            dataErrorValue.setVisibility(View.VISIBLE);
            dataErrorValue.setText(UiKit.errorLabel(this, errorCode) + " "
                    + getString(R.string.sync_error_kept));
        }
        syncButton.setText(syncing ? R.string.syncing : R.string.sync_now);
        syncButton.setEnabled(!syncing);
    }

    private void syncNow() {
        if (syncing) {
            return;
        }
        if (SettingsRepository.PROVIDER_NONE.equals(settings.providerId())) {
            Toast.makeText(this, R.string.error_no_provider, Toast.LENGTH_SHORT).show();
            return;
        }
        syncing = true;
        loadState();
        executor.execute(() -> {
            ZoneId zone = repository.zone();
            LocalDate today = LocalDate.now(zone);
            SyncResult result = repository.sync(today.minusDays(1), 3);
            handler.post(() -> {
                syncing = false;
                loadState();
                loadCorrectionDay();
                if (result.success) {
                    Toast.makeText(SettingsActivity.this,
                            getString(R.string.sync_success, result.savedDays),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this,
                            getString(R.string.sync_error,
                                    UiKit.errorLabel(SettingsActivity.this, result.errorCode)),
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void pickCorrectionDate() {
        LocalDate current = correctionDate == null
                ? LocalDate.now(repository.zone()) : correctionDate;
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            correctionDate = LocalDate.of(year, month + 1, day);
            loadCorrectionDay();
        }, current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth());
        picker.setTitle(getString(R.string.correction_date_title));
        picker.show();
    }

    private void pickCorrectionTime() {
        if (correctionTime == null || selectedCorrectionDay == null) {
            return;
        }
        ZonedDateTime local = correctionTime.atZone(selectedCorrectionDay.zone());
        TimePickerDialog picker = new TimePickerDialog(this, (view, hour, minute) -> {
            correctionTime = selectedCorrectionDay.date().atTime(hour, minute)
                    .atZone(selectedCorrectionDay.zone()).toInstant();
            correctionTimeButton.setText(DateTimeText.time(correctionTime,
                    selectedCorrectionDay.zone(), locale()));
        }, local.getHour(), local.getMinute(), DateFormat.is24HourFormat(this));
        picker.setTitle(getString(R.string.correction_time_title));
        picker.show();
    }

    private void loadCorrectionDay() {
        if (correctionDate == null) {
            correctionDate = LocalDate.now(repository.zone());
        }
        correctionDateButton.setText(DateTimeText.gregorianMedium(correctionDate, locale()));
        correctionError.setVisibility(View.GONE);
        selectedCorrectionDay = repository.load(correctionDate);
        Prayer prayer = selectedPrayer();
        if (selectedCorrectionDay == null) {
            correctionOriginalValue.setText(R.string.correction_no_day);
            correctionTimeButton.setEnabled(false);
            correctionTimeButton.setText("—");
            correctionReasonInput.setText("");
            correctionProvenanceInput.setText("");
            findViewById(R.id.saveCorrectionButton).setEnabled(false);
            renderCorrections(null);
            return;
        }
        Instant original = selectedCorrectionDay.originalTimes().get(prayer);
        correctionTime = selectedCorrectionDay.effectiveTimes().get(prayer);
        correctionOriginalValue.setText(original == null
                ? getString(R.string.correction_no_day)
                : getString(R.string.original_value,
                DateTimeText.time(original, selectedCorrectionDay.zone(), locale())));
        correctionTimeButton.setEnabled(original != null);
        correctionTimeButton.setText(correctionTime == null ? "—"
                : DateTimeText.time(correctionTime, selectedCorrectionDay.zone(), locale()));

        Correction existing = selectedCorrectionDay.corrections().get(prayer);
        correctionReasonInput.setText(existing == null ? "" : existing.reason());
        correctionProvenanceInput.setText(existing == null
                ? getString(R.string.manual_source) : existing.provenance());
        findViewById(R.id.saveCorrectionButton).setEnabled(original != null);
        renderCorrections(selectedCorrectionDay);
    }

    private void saveCorrection() {
        Prayer prayer = selectedPrayer();
        if (selectedCorrectionDay == null || correctionTime == null) {
            showCorrectionError(R.string.correction_no_day);
            return;
        }
        String reason = correctionReasonInput.getText().toString().trim();
        String provenance = correctionProvenanceInput.getText().toString().trim();
        if (reason.isEmpty() || provenance.isEmpty()) {
            showCorrectionError(R.string.correction_required);
            return;
        }
        PrayerDay updated = repository.saveCorrection(correctionDate, prayer,
                correctionTime, reason, provenance);
        if (updated == null) {
            showCorrectionError(R.string.correction_no_day);
            return;
        }
        Toast.makeText(this, R.string.correction_saved, Toast.LENGTH_SHORT).show();
        loadCorrectionDay();
        loadState();
    }

    private void showCorrectionError(int message) {
        correctionError.setText(message);
        correctionError.setVisibility(View.VISIBLE);
    }

    private void renderCorrections(PrayerDay day) {
        correctionsContainer.removeAllViews();
        if (day == null || day.corrections().isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_corrections);
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(12);
            correctionsContainer.addView(empty);
            return;
        }
        for (Prayer prayer : Prayer.ALL) {
            Correction correction = day.corrections().get(prayer);
            if (correction == null) {
                continue;
            }
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card_alt);
            card.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 10),
                    UiKit.dp(this, 12), UiKit.dp(this, 10));

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = new TextView(this);
            title.setText(prayerName(prayer) + " · " + getString(R.string.corrected_badge));
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(14);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            header.addView(title);
            Button clear = new Button(this, null, 0, R.style.HorlogeButton_Ghost);
            clear.setText(R.string.clear_correction);
            clear.setAllCaps(false);
            clear.setTextSize(12);
            clear.setOnClickListener(v -> {
                repository.clearCorrection(correctionDate, prayer);
                Toast.makeText(this, R.string.correction_cleared, Toast.LENGTH_SHORT).show();
                loadCorrectionDay();
            });
            header.addView(clear);
            card.addView(header);

            TextView values = new TextView(this);
            values.setText(getString(R.string.original_value,
                    DateTimeText.time(correction.original(), day.zone(), locale()))
                    + "  ·  " + getString(R.string.corrected_value,
                    DateTimeText.time(correction.corrected(), day.zone(), locale())));
            values.setTextColor(getColor(R.color.text_secondary));
            values.setTextSize(12);
            card.addView(values);

            TextView reason = new TextView(this);
            reason.setText(correction.reason());
            reason.setTextColor(getColor(R.color.text_primary));
            reason.setTextSize(12);
            card.addView(reason);

            TextView provenance = new TextView(this);
            provenance.setText(getString(R.string.correction_provenance_item,
                    correction.provenance()));
            provenance.setTextColor(getColor(R.color.text_secondary));
            provenance.setTextSize(11);
            card.addView(provenance);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = UiKit.dp(this, 6);
            correctionsContainer.addView(card, params);
        }
    }

    private Prayer selectedPrayer() {
        int index = correctionPrayerSpinner.getSelectedItemPosition();
        if (index < 0 || index >= Prayer.ALL.size()) {
            return Prayer.FAJR;
        }
        return Prayer.ALL.get(index);
    }

    private String colorLabel(ColorSlot slot) {
        switch (slot) {
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

    private abstract static class SimpleItemSelected
            implements android.widget.AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                   int position, long id) {
            onItemSelected(position);
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }

        public abstract void onItemSelected(int position);
    }

    private static final class TintedSpinnerAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final int textColor;
        private final int surfaceColor;

        TintedSpinnerAdapter(Context context, List<String> items) {
            super(context, android.R.layout.simple_spinner_item, items);
            this.context = context;
            this.textColor = context.getColor(R.color.text_primary);
            this.surfaceColor = context.getColor(R.color.surface);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            style(view);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            style(view);
            view.setBackgroundColor(surfaceColor);
            return view;
        }

        private void style(View view) {
            if (view instanceof TextView) {
                TextView text = (TextView) view;
                text.setTextColor(textColor);
                text.setTextSize(15);
                text.setPadding(UiKit.dp(context, 10), UiKit.dp(context, 10),
                        UiKit.dp(context, 10), UiKit.dp(context, 10));
            }
        }
    }
}
