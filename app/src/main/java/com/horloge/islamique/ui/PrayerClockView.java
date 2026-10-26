package com.horloge.islamique.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.horloge.islamique.R;
import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.TimeEngine;
import com.horloge.islamique.core.TimeInterval;
import com.horloge.islamique.settings.AppLanguage;
import com.horloge.islamique.settings.ColorSlot;
import com.horloge.islamique.settings.SettingsRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 12-hour dial. Each ring contains an AM lane (dashed, inner) and a PM lane
 * (solid, outer). Ring 1 identifies prayers; rings 2 and 3 only show explicit
 * imported intervals and are never inferred from prayer times.
 */
public final class PrayerClockView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private SettingsRepository settings;
    private Locale locale = new Locale("ar");
    private AppLanguage language = AppLanguage.ARABIC;

    private PrayerDay day;
    private List<TimeInterval> prayerSectors = Collections.emptyList();
    private Instant now = Instant.now();
    private Prayer activePrayer;
    private LocalDate activePrayerDate;

    private int colorFajr;
    private int colorDhuhr;
    private int colorAsr;
    private int colorMaghrib;
    private int colorIsha;
    private int colorOptional;
    private int colorNecessary;

    private int textPrimary;
    private int textSecondary;
    private int faceColor;
    private int faceAltColor;
    private int ringTrack;
    private int ringTrackStrong;
    private int accent;

    public PrayerClockView(Context context) {
        this(context, null);
    }

    public PrayerClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        reloadThemeColors();
    }

    public void configure(SettingsRepository settings, AppLanguage language,
                          PrayerDay day, PrayerDay nextDay) {
        this.settings = settings;
        this.language = language;
        this.locale = language.locale();
        this.day = day;
        if (day != null) {
            this.prayerSectors = TimeEngine.prayerSectors(day, nextDay);
        } else {
            this.prayerSectors = Collections.emptyList();
        }
        reloadPalette();
        reloadThemeColors();
        invalidate();
    }

    public void setNow(Instant now) {
        this.now = now;
        invalidate();
    }

    public void setActivePrayer(Prayer prayer, LocalDate date) {
        this.activePrayer = prayer;
        this.activePrayerDate = date;
        invalidate();
    }

    private void reloadPalette() {
        if (settings == null) {
            return;
        }
        colorFajr = settings.color(ColorSlot.FAJR);
        colorDhuhr = settings.color(ColorSlot.DHUHR);
        colorAsr = settings.color(ColorSlot.ASR);
        colorMaghrib = settings.color(ColorSlot.MAGHRIB);
        colorIsha = settings.color(ColorSlot.ISHA);
        colorOptional = settings.color(ColorSlot.OPTIONAL);
        colorNecessary = settings.color(ColorSlot.NECESSARY);
    }

    private void reloadThemeColors() {
        textPrimary = getContext().getColor(R.color.text_primary);
        textSecondary = getContext().getColor(R.color.text_secondary);
        faceColor = getContext().getColor(R.color.clock_face);
        faceAltColor = getContext().getColor(R.color.clock_face_alt);
        ringTrack = getContext().getColor(R.color.ring_track);
        ringTrackStrong = getContext().getColor(R.color.ring_track_strong);
        accent = getContext().getColor(R.color.accent);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width == 0) {
            width = UiKit.dp(getContext(), 340);
        }
        int target = Math.min(width, UiKit.dp(getContext(), 440));
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            target = Math.min(target, MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(width, target);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode()) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float base = Math.min(width, height) / 2f - UiKit.dp(getContext(), 4);
        if (base <= 0) {
            return;
        }

        drawFace(canvas, cx, cy, base);
        drawOuterTicks(canvas, cx, cy, base);
        drawTracks(canvas, cx, cy, base);
        drawPrayerRing(canvas, cx, cy, base);
        drawIntervalRing(canvas, cx, cy, base * 0.775f, base * 0.72f,
                day == null ? Collections.emptyList() : day.optionalIntervals(),
                colorOptional, true);
        drawIntervalRing(canvas, cx, cy, base * 0.655f, base * 0.60f,
                day == null ? Collections.emptyList() : day.necessaryIntervals(),
                colorNecessary, false);
        drawNumerals(canvas, cx, cy, base);
        drawHands(canvas, cx, cy, base);
        drawAmPm(canvas, cx, cy, base);
    }

    private void drawFace(Canvas canvas, float cx, float cy, float base) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx, cy, base,
                faceAltColor, faceColor, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, base, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(UiKit.dp(getContext(), 1.5f));
        paint.setColor(ringTrackStrong);
        canvas.drawCircle(cx, cy, base * 0.995f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(UiKit.dp(getContext(), 1));
        paint.setColor(ringTrack);
        canvas.drawCircle(cx, cy, base * 0.985f, paint);
    }

    private void drawOuterTicks(Canvas canvas, float cx, float cy, float base) {
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 60; i++) {
            boolean major = i % 5 == 0;
            float angle = (float) Math.toRadians(i * 6 - 90);
            float outer = base * 0.985f;
            float inner = base * (major ? 0.952f : 0.966f);
            paint.setColor(major ? ringTrackStrong : ringTrack);
            paint.setStrokeWidth(UiKit.dp(getContext(), major ? 2f : 1f));
            canvas.drawLine(
                    cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer,
                    cy + (float) Math.sin(angle) * outer,
                    paint);
        }
    }

    private void drawTracks(Canvas canvas, float cx, float cy, float base) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setPathEffect(null);
        float[] radii = {
                base * 0.91f, base * 0.855f,
                base * 0.775f, base * 0.72f,
                base * 0.655f, base * 0.60f
        };
        float[] widths = {
                base * 0.035f, base * 0.022f,
                base * 0.035f, base * 0.022f,
                base * 0.035f, base * 0.022f
        };
        for (int i = 0; i < radii.length; i++) {
            paint.setColor(ringTrack);
            paint.setStrokeWidth(Math.max(widths[i], UiKit.dp(getContext(), 2.5f)));
            canvas.drawCircle(cx, cy, radii[i], paint);
        }
    }

    private void drawPrayerRing(Canvas canvas, float cx, float cy, float base) {
        if (day == null || prayerSectors.isEmpty()) {
            return;
        }
        Paint colorPaint = segmentPaint();
        Paint haloPaint = segmentPaint();
        for (TimeInterval sector : prayerSectors) {
            Prayer prayer = Prayer.fromKey(sector.id().substring("prayer-".length()));
            if (prayer == null) {
                continue;
            }
            int color = prayerColor(prayer);
            drawInterval(canvas, cx, cy, base * 0.91f, base * 0.855f,
                    sector, color, colorPaint, haloPaint, true, false);
            if (prayer == activePrayer && day.date().equals(activePrayerDate)) {
                drawActiveOverlay(canvas, cx, cy, base * 0.91f, base * 0.855f,
                        sector, true);
            }
        }
    }

    private void drawIntervalRing(Canvas canvas, float cx, float cy,
                                  float pmRadius, float amRadius,
                                  List<TimeInterval> intervals, int color,
                                  boolean dashed) {
        if (intervals == null || intervals.isEmpty()) {
            return;
        }
        Paint colorPaint = segmentPaint();
        Paint haloPaint = segmentPaint();
        for (TimeInterval interval : intervals) {
            if (!interval.hasValidRange()) {
                continue;
            }
            drawInterval(canvas, cx, cy, pmRadius, amRadius,
                    interval, color, colorPaint, haloPaint, false, dashed);
            if (interval.contains(now)) {
                drawActiveOverlay(canvas, cx, cy, pmRadius, amRadius,
                        interval, false);
            }
        }
    }

    private Paint segmentPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        return p;
    }

    private void drawInterval(Canvas canvas, float cx, float cy,
                              float pmRadius, float amRadius,
                              TimeInterval interval, int color,
                              Paint colorPaint, Paint haloPaint,
                              boolean prayerRing, boolean dashed) {
        if (day == null) {
            return;
        }
        ZoneId zone = day.zone();
        Instant dayStart = day.date().atStartOfDay(zone).toInstant();
        Instant dayEnd = day.date().plusDays(1).atStartOfDay(zone).toInstant();
        long clippedStart = Math.max(interval.start().getEpochSecond(),
                dayStart.getEpochSecond());
        long clippedEnd = Math.min(interval.end().getEpochSecond(),
                dayEnd.getEpochSecond());
        if (clippedEnd <= clippedStart) {
            return;
        }
        long dayStartSec = dayStart.getEpochSecond();
        long dayEndSec = dayEnd.getEpochSecond();
        long startSec = clippedStart == dayStartSec ? 0 : localSecondOfDay(clippedStart, zone);
        long endSec = clippedEnd == dayEndSec ? 86400 : localSecondOfDay(clippedEnd, zone);
        if (endSec <= startSec) {
            return;
        }

        if (startSec < 43200) {
            drawLane(canvas, cx, cy, amRadius, color, colorPaint, haloPaint,
                    startSec, Math.min(endSec, 43200), dashed, prayerRing);
        }
        if (endSec > 43200) {
            drawLane(canvas, cx, cy, pmRadius, color, colorPaint, haloPaint,
                    Math.max(startSec, 43200), endSec, false, prayerRing);
        }
    }

    private long localSecondOfDay(long epochSecond, ZoneId zone) {
        ZonedDateTime local = Instant.ofEpochSecond(epochSecond).atZone(zone);
        return local.toLocalTime().toSecondOfDay();
    }

    private void drawLane(Canvas canvas, float cx, float cy, float radius, int color,
                          Paint colorPaint, Paint haloPaint, long startSec,
                          long endSec, boolean dashed, boolean prayerRing) {
        if (endSec <= startSec) {
            return;
        }
        float startAngle = (float) (startSec % 43200L) / 43200f * 360f - 90f;
        float sweep = (float) (endSec - startSec) / 43200f * 360f;
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        haloPaint.setColor(textPrimary);
        haloPaint.setAlpha(prayerRing ? 34 : 46);
        haloPaint.setStrokeWidth(Math.max(radius * 0.055f, UiKit.dp(getContext(), 5)));
        haloPaint.setPathEffect(null);
        canvas.drawArc(arcRect, startAngle, sweep, false, haloPaint);

        colorPaint.setColor(color);
        colorPaint.setAlpha(255);
        colorPaint.setStrokeWidth(Math.max(radius * 0.043f, UiKit.dp(getContext(), 3)));
        if (dashed) {
            float dash = Math.max(UiKit.dp(getContext(), 2.2f), radius * 0.022f);
            colorPaint.setPathEffect(new DashPathEffect(new float[]{dash, dash * 1.1f}, 0));
        } else {
            colorPaint.setPathEffect(null);
        }
        canvas.drawArc(arcRect, startAngle, sweep, false, colorPaint);
    }

    private void drawActiveOverlay(Canvas canvas, float cx, float cy,
                                   float pmRadius, float amRadius,
                                   TimeInterval interval, boolean prayerRing) {
        if (day == null) {
            return;
        }
        ZoneId zone = day.zone();
        Instant dayStart = day.date().atStartOfDay(zone).toInstant();
        Instant dayEnd = day.date().plusDays(1).atStartOfDay(zone).toInstant();
        long clippedStart = Math.max(interval.start().getEpochSecond(),
                dayStart.getEpochSecond());
        long clippedEnd = Math.min(interval.end().getEpochSecond(),
                dayEnd.getEpochSecond());
        if (clippedEnd <= clippedStart) {
            return;
        }
        long dayStartSec = dayStart.getEpochSecond();
        long dayEndSec = dayEnd.getEpochSecond();
        long startSec = clippedStart == dayStartSec ? 0 : localSecondOfDay(clippedStart, zone);
        long endSec = clippedEnd == dayEndSec ? 86400 : localSecondOfDay(clippedEnd, zone);
        Paint overlay = segmentPaint();
        overlay.setColor(textPrimary);
        overlay.setAlpha(220);
        overlay.setStrokeWidth(UiKit.dp(getContext(), 2));
        overlay.setPathEffect(null);
        if (startSec < 43200) {
            drawOverlaySegment(canvas, cx, cy, amRadius, overlay,
                    startSec, Math.min(endSec, 43200));
        }
        if (endSec > 43200) {
            drawOverlaySegment(canvas, cx, cy, pmRadius, overlay,
                    Math.max(startSec, 43200), endSec);
        }
    }

    private void drawOverlaySegment(Canvas canvas, float cx, float cy, float radius,
                                    Paint overlay, long startSec, long endSec) {
        if (endSec <= startSec) {
            return;
        }
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        float startAngle = (float) (startSec % 43200L) / 43200f * 360f - 90f;
        float sweep = (float) (endSec - startSec) / 43200f * 360f;
        canvas.drawArc(arcRect, startAngle, sweep, false, overlay);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accent);
        float midAngle = startAngle + sweep / 2f;
        float midRad = (float) Math.toRadians(midAngle);
        canvas.drawCircle(cx + (float) Math.cos(midRad) * radius,
                cy + (float) Math.sin(midRad) * radius,
                UiKit.dp(getContext(), 2.5f), paint);
    }

    private void drawNumerals(Canvas canvas, float cx, float cy, float base) {
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(textPrimary);
        textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(base * 0.082f);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float yOffset = -(metrics.ascent + metrics.descent) / 2f;
        float radius = base * 0.50f;
        for (int i = 1; i <= 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);
            String label = DateTimeTextLocal.digits(i, locale);
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius + yOffset;
            canvas.drawText(label, x, y, textPaint);
        }

        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        textPaint.setTextSize(base * 0.042f);
        textPaint.setColor(textSecondary);
        String am = getContext().getString(R.string.am);
        String pm = getContext().getString(R.string.pm);
        String label = now.atZone(day == null ? ZoneId.systemDefault() : day.zone())
                .getHour() < 12 ? am : pm;
        canvas.drawText(label, cx, cy + base * 0.31f, textPaint);
    }

    private void drawHands(Canvas canvas, float cx, float cy, float base) {
        ZoneId zone = day == null ? ZoneId.systemDefault() : day.zone();
        ZonedDateTime local = now.atZone(zone);
        float seconds = local.getSecond() + local.getNano() / 1_000_000_000f;
        float minutes = local.getMinute() + seconds / 60f;
        float hours = (local.getHour() % 12) + minutes / 60f;

        float hourAngle = (float) Math.toRadians(hours * 30f - 90f);
        float minuteAngle = (float) Math.toRadians(minutes * 6f - 90f);
        float secondAngle = (float) Math.toRadians(seconds * 6f - 90f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        paint.setColor(textPrimary);
        paint.setStrokeWidth(UiKit.dp(getContext(), 5.5f));
        canvas.drawLine(cx, cy,
                cx + (float) Math.cos(hourAngle) * base * 0.30f,
                cy + (float) Math.sin(hourAngle) * base * 0.30f, paint);

        paint.setStrokeWidth(UiKit.dp(getContext(), 3.2f));
        canvas.drawLine(cx, cy,
                cx + (float) Math.cos(minuteAngle) * base * 0.43f,
                cy + (float) Math.sin(minuteAngle) * base * 0.43f, paint);

        paint.setColor(accent);
        paint.setStrokeWidth(UiKit.dp(getContext(), 1.4f));
        canvas.drawLine(cx - (float) Math.cos(secondAngle) * base * 0.06f,
                cy - (float) Math.sin(secondAngle) * base * 0.06f,
                cx + (float) Math.cos(secondAngle) * base * 0.46f,
                cy + (float) Math.sin(secondAngle) * base * 0.46f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textPrimary);
        canvas.drawCircle(cx, cy, UiKit.dp(getContext(), 4.5f), paint);
        paint.setColor(accent);
        canvas.drawCircle(cx, cy, UiKit.dp(getContext(), 2f), paint);
    }

    private void drawAmPm(Canvas canvas, float cx, float cy, float base) {
        // The AM/PM badge is drawn in drawNumerals; this method keeps a small top marker
        // so the active half of the dial is visible even with the hands over the centre.
        ZoneId zone = day == null ? ZoneId.systemDefault() : day.zone();
        boolean am = now.atZone(zone).getHour() < 12;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(am ? ringTrackStrong : accent);
        paint.setAlpha(180);
        canvas.drawCircle(cx, cy - base * 0.31f, UiKit.dp(getContext(), 3f), paint);
    }

    private int prayerColor(Prayer prayer) {
        switch (prayer) {
            case FAJR:
                return colorFajr;
            case DHUHR:
                return colorDhuhr;
            case ASR:
                return colorAsr;
            case MAGHRIB:
                return colorMaghrib;
            case ISHA:
            default:
                return colorIsha;
        }
    }

    private static final class DateTimeTextLocal {
        static String digits(int value, Locale locale) {
            String raw = Integer.toString(value);
            if (!"ar".equals(locale.getLanguage())) {
                return raw;
            }
            char[] arabic = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < raw.length(); i++) {
                builder.append(arabic[raw.charAt(i) - '0']);
            }
            return builder.toString();
        }
    }
}
