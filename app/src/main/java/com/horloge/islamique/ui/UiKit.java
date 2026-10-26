package com.horloge.islamique.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.horloge.islamique.R;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.SourceInfo;

/** Small UI helpers shared by activities. */
public final class UiKit {

    private UiKit() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static void tintBackground(View view, int color) {
        Drawable background = view.getBackground();
        if (background != null) {
            background = background.mutate();
            background.setTint(color);
            view.setBackground(background);
        }
    }

    public static View coloredDot(Context context, int color) {
        View dot = new View(context);
        Drawable drawable = context.getDrawable(R.drawable.bg_dot).mutate();
        drawable.setTint(color);
        dot.setBackground(drawable);
        return dot;
    }

    public static LinearLayout.LayoutParams dotParams(Context context) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dp(context, 14), dp(context, 14));
        params.setMarginEnd(dp(context, 10));
        params.gravity = Gravity.CENTER_VERTICAL;
        return params;
    }

    public static TextView badge(Context context, String text, int textColor) {
        TextView badge = new TextView(context);
        badge.setText(text);
        badge.setTextColor(textColor);
        badge.setTextSize(10);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.bg_badge);
        int horizontal = dp(context, 7);
        int vertical = dp(context, 2);
        badge.setPadding(horizontal, vertical, horizontal, vertical);
        return badge;
    }

    public static int statusColor(Context context, PrayerDay.DataFreshness freshness) {
        switch (freshness) {
            case CURRENT:
                return context.getColor(R.color.success);
            case STALE:
                return context.getColor(R.color.warning);
            case UNAVAILABLE:
                return context.getColor(R.color.danger);
            case UNVERIFIED:
            default:
                return context.getColor(R.color.warning);
        }
    }

    public static int statusBackground(Context context, PrayerDay.DataFreshness freshness) {
        switch (freshness) {
            case CURRENT:
                return context.getColor(R.color.status_bg);
            case STALE:
            case UNVERIFIED:
                return context.getColor(R.color.status_stale_bg);
            case UNAVAILABLE:
            default:
                return context.getColor(R.color.status_error_bg);
        }
    }

    public static int statusLabel(PrayerDay.DataFreshness freshness) {
        switch (freshness) {
            case CURRENT:
                return R.string.status_current;
            case STALE:
                return R.string.status_stale;
            case UNAVAILABLE:
                return R.string.status_unavailable;
            case UNVERIFIED:
            default:
                return R.string.status_unverified;
        }
    }

    public static String statusSymbol(PrayerDay.DataFreshness freshness) {
        switch (freshness) {
            case CURRENT:
                return "✓";
            case STALE:
                return "!";
            case UNAVAILABLE:
                return "×";
            case UNVERIFIED:
            default:
                return "?";
        }
    }

    public static int sourceKindLabel(SourceInfo.Kind kind) {
        switch (kind) {
            case OFFICIAL:
                return R.string.source_kind_official;
            case CALCULATED:
                return R.string.source_kind_calculated;
            case ESTIMATED:
                return R.string.source_kind_estimated;
            case MANUAL:
                return R.string.source_kind_manual;
            case UNVERIFIED:
            default:
                return R.string.source_kind_unverified;
        }
    }

    public static String providerLabel(Context context, SourceInfo source) {
        if (source == null) {
            return context.getString(R.string.unknown);
        }
        if ("aladhan".equals(source.id())) {
            return context.getString(R.string.provider_aladhan_short);
        }
        if ("manual".equals(source.id())) {
            return context.getString(R.string.manual_source);
        }
        return source.displayName();
    }

    public static String errorLabel(Context context, String code) {
        if (code == null) {
            return context.getString(R.string.error_unknown);
        }
        switch (code) {
            case "network":
                return context.getString(R.string.error_network);
            case "http":
                return context.getString(R.string.error_http);
            case "invalid_data":
                return context.getString(R.string.error_invalid_data);
            case "timezone":
                return context.getString(R.string.error_timezone);
            case "storage":
                return context.getString(R.string.error_storage);
            case "no_provider":
                return context.getString(R.string.error_no_provider);
            default:
                return context.getString(R.string.error_unknown);
        }
    }

    public static ColorStateList tint(int color) {
        return ColorStateList.valueOf(color);
    }

    public static View divider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(context.getColor(R.color.outline));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1));
        params.topMargin = dp(context, 8);
        params.bottomMargin = dp(context, 8);
        view.setLayoutParams(params);
        return view;
    }

    public static double contrastRatio(int first, int second) {
        double l1 = relativeLuminance(first);
        double l2 = relativeLuminance(second);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(int color) {
        double r = channel((color >> 16) & 0xFF);
        double g = channel((color >> 8) & 0xFF);
        double b = channel(color & 0xFF);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(int value) {
        double normalized = value / 255.0;
        return normalized <= 0.03928
                ? normalized / 12.92
                : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }
}
