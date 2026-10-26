package com.horloge.islamique.ui;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.horloge.islamique.R;
import com.horloge.islamique.core.Correction;
import com.horloge.islamique.core.DateTimeText;
import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.data.PrayerRepository;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

/** Correction dialog shared by the main screen and settings. */
public final class CorrectionDialog {

    private CorrectionDialog() {
    }

    public static void show(BaseActivity activity, PrayerRepository repository,
                            PrayerDay day, Prayer prayer, Runnable onChanged) {
        if (day == null) {
            Toast.makeText(activity, R.string.correction_no_day, Toast.LENGTH_SHORT).show();
            return;
        }
        Instant original = day.originalTimes().get(prayer);
        if (original == null) {
            Toast.makeText(activity, R.string.correction_no_day, Toast.LENGTH_SHORT).show();
            return;
        }
        Locale locale = activity.locale();
        ZoneId zone = day.zone();
        Correction existing = day.corrections().get(prayer);
        Instant[] selected = {existing == null ? day.effectiveTimes().get(prayer) : existing.corrected()};

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = UiKit.dp(activity, 18);
        root.setPadding(padding, padding, padding, UiKit.dp(activity, 6));

        TextView subtitle = new TextView(activity);
        subtitle.setText(R.string.correction_subtitle);
        subtitle.setTextColor(activity.getColor(R.color.text_secondary));
        subtitle.setTextSize(13);
        root.addView(subtitle);

        TextView originalLabel = new TextView(activity);
        originalLabel.setText(activity.getString(R.string.original_value,
                DateTimeText.time(original, zone, locale)));
        originalLabel.setTextColor(activity.getColor(R.color.text_primary));
        originalLabel.setTextSize(14);
        originalLabel.setPadding(0, UiKit.dp(activity, 10), 0, 0);
        root.addView(originalLabel);

        TextView timeLabel = new TextView(activity);
        timeLabel.setText(R.string.new_time_label);
        timeLabel.setTextColor(activity.getColor(R.color.text_secondary));
        timeLabel.setTextSize(13);
        timeLabel.setPadding(0, UiKit.dp(activity, 10), 0, UiKit.dp(activity, 2));
        root.addView(timeLabel);

        Button timeButton = new Button(activity, null, 0, R.style.HorlogeButton_Ghost);
        timeButton.setText(DateTimeText.time(selected[0], zone, locale));
        timeButton.setAllCaps(false);
        root.addView(timeButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText reason = addInput(activity, root, R.string.reason_label,
                existing == null ? "" : existing.reason(), true);
        EditText provenance = addInput(activity, root, R.string.provenance_label,
                existing == null ? activity.getString(R.string.manual_source) : existing.provenance(),
                false);

        TextView error = new TextView(activity);
        error.setTextColor(activity.getColor(R.color.danger));
        error.setTextSize(12);
        error.setVisibility(TextView.GONE);
        root.addView(error);

        timeButton.setOnClickListener(v -> {
            ZonedDateTime local = selected[0].atZone(zone);
            TimePickerDialog picker = new TimePickerDialog(activity,
                    (view, hourOfDay, minute) -> {
                        selected[0] = day.date().atTime(hourOfDay, minute)
                                .atZone(zone).toInstant();
                        timeButton.setText(DateTimeText.time(selected[0], zone, locale));
                    },
                    local.getHour(), local.getMinute(),
                    DateFormat.is24HourFormat(activity));
            picker.setTitle(activity.getString(R.string.correction_time_title));
            picker.show();
        });

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(root);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.correction_title)
                .setView(scrollView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null);
        if (existing != null) {
            builder.setNeutralButton(R.string.clear_correction, null);
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.save);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String reasonText = reason.getText().toString().trim();
                String provenanceText = provenance.getText().toString().trim();
                if (reasonText.isEmpty() || provenanceText.isEmpty()) {
                    error.setText(R.string.correction_required);
                    error.setVisibility(TextView.VISIBLE);
                    return;
                }
                PrayerDay updated = repository.saveCorrection(day.date(), prayer,
                        selected[0], reasonText, provenanceText);
                if (updated == null) {
                    error.setText(R.string.correction_no_day);
                    error.setVisibility(TextView.VISIBLE);
                    return;
                }
                Toast.makeText(activity, R.string.correction_saved, Toast.LENGTH_SHORT).show();
                if (onChanged != null) {
                    onChanged.run();
                }
                dialog.dismiss();
            });
            if (existing != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    repository.clearCorrection(day.date(), prayer);
                    Toast.makeText(activity, R.string.correction_cleared, Toast.LENGTH_SHORT).show();
                    if (onChanged != null) {
                        onChanged.run();
                    }
                    dialog.dismiss();
                });
            }
        });
        dialog.show();
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.save);
        }
    }

    private static EditText addInput(Context context, LinearLayout root,
                                     int hint, String initial, boolean multiline) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setText(initial);
        input.setTextSize(14);
        input.setTextColor(context.getColor(R.color.text_primary));
        input.setHintTextColor(context.getColor(R.color.text_secondary));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(UiKit.dp(context, 12), UiKit.dp(context, 10),
                UiKit.dp(context, 12), UiKit.dp(context, 10));
        input.setInputType(multiline
                ? android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                : android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (multiline) {
            input.setMaxLines(3);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = UiKit.dp(context, 6);
        root.addView(input, params);
        return input;
    }
}
