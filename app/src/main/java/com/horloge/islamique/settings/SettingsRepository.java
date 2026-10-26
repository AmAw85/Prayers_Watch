package com.horloge.islamique.settings;

import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persisted user preferences. Default language is Arabic. */
public final class SettingsRepository {
    public static final String PREFS_NAME = "horloge_settings";
    public static final String DEFAULT_LANGUAGE = "ar";
    public static final String PROVIDER_ALADHAN = "aladhan";
    public static final String PROVIDER_NONE = "none";
    public static final String DEFAULT_CITY = "algiers";
    public static final String DEFAULT_TIMEZONE = "Africa/Algiers";

    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_PROVIDER = "provider_id";
    private static final String KEY_CITY = "city_id";
    private static final String KEY_TIMEZONE = "timezone_id";
    private static final String KEY_LAST_SUCCESS = "last_sync_success";
    private static final String KEY_LAST_ATTEMPT = "last_sync_attempt";
    private static final String KEY_LAST_ERROR = "last_sync_error";

    private final KeyValueStore store;

    public SettingsRepository(KeyValueStore store) {
        this.store = store;
    }

    public AppLanguage language() {
        return AppLanguage.fromCode(store.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE));
    }

    public void setLanguage(AppLanguage language) {
        store.putString(KEY_LANGUAGE, (language == null ? AppLanguage.ARABIC : language).code());
    }

    public Map<ColorSlot, Integer> colors() {
        Map<ColorSlot, Integer> result = new LinkedHashMap<>();
        for (ColorSlot slot : ColorSlot.values()) {
            result.put(slot, color(slot));
        }
        return result;
    }

    public int color(ColorSlot slot) {
        int stored = store.getInt(slot.preferenceKey(), slot.defaultColor());
        return stored | 0xFF000000;
    }

    public String colorHex(ColorSlot slot) {
        return String.format("#%06X", color(slot) & 0xFFFFFF);
    }

    public void setColor(ColorSlot slot, int color) {
        store.putInt(slot.preferenceKey(), color | 0xFF000000);
    }

    public void resetColors() {
        for (ColorSlot slot : ColorSlot.values()) {
            store.remove(slot.preferenceKey());
        }
    }

    public String providerId() {
        String value = store.getString(KEY_PROVIDER, PROVIDER_ALADHAN);
        return PROVIDER_NONE.equals(value) ? PROVIDER_NONE : PROVIDER_ALADHAN;
    }

    public void setProviderId(String providerId) {
        store.putString(KEY_PROVIDER, PROVIDER_NONE.equals(providerId)
                ? PROVIDER_NONE : PROVIDER_ALADHAN);
    }

    public String cityId() {
        return store.getString(KEY_CITY, DEFAULT_CITY);
    }

    public void setCityId(String cityId) {
        store.putString(KEY_CITY, cityId == null || cityId.isEmpty() ? DEFAULT_CITY : cityId);
    }

    public String timezoneId() {
        String value = store.getString(KEY_TIMEZONE, DEFAULT_TIMEZONE);
        try {
            ZoneId.of(value);
            return value;
        } catch (RuntimeException ex) {
            return DEFAULT_TIMEZONE;
        }
    }

    public void setTimezoneId(String timezone) {
        try {
            store.putString(KEY_TIMEZONE, ZoneId.of(timezone).getId());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid IANA timezone", ex);
        }
    }

    public Instant lastSuccessfulSync() {
        long value = store.getLong(KEY_LAST_SUCCESS, 0L);
        return value <= 0 ? null : Instant.ofEpochMilli(value);
    }

    public void markSyncSuccess(Instant at) {
        store.putLong(KEY_LAST_SUCCESS, at.toEpochMilli());
        store.putLong(KEY_LAST_ATTEMPT, at.toEpochMilli());
        store.putString(KEY_LAST_ERROR, "");
    }

    public Instant lastSyncAttempt() {
        long value = store.getLong(KEY_LAST_ATTEMPT, 0L);
        return value <= 0 ? null : Instant.ofEpochMilli(value);
    }

    public void markSyncAttempt(Instant at) {
        store.putLong(KEY_LAST_ATTEMPT, at.toEpochMilli());
    }

    public void markSyncError(Instant at, String errorCode) {
        store.putLong(KEY_LAST_ATTEMPT, at.toEpochMilli());
        store.putString(KEY_LAST_ERROR, errorCode == null ? "unknown" : errorCode);
    }

    public String lastSyncError() {
        return store.getString(KEY_LAST_ERROR, "");
    }

    public void clearSyncError() {
        store.putString(KEY_LAST_ERROR, "");
    }
}
