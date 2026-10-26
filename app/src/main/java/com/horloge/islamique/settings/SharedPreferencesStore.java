package com.horloge.islamique.settings;

import android.content.SharedPreferences;

/** Android-backed key/value store. */
public final class SharedPreferencesStore implements KeyValueStore {
    private final SharedPreferences preferences;

    public SharedPreferencesStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getString(String key, String fallback) {
        return preferences.getString(key, fallback);
    }

    @Override
    public int getInt(String key, int fallback) {
        return preferences.getInt(key, fallback);
    }

    @Override
    public long getLong(String key, long fallback) {
        return preferences.getLong(key, fallback);
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return preferences.getBoolean(key, fallback);
    }

    @Override
    public void putString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    @Override
    public void putInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    @Override
    public void putLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }

    @Override
    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    @Override
    public void remove(String key) {
        preferences.edit().remove(key).apply();
    }
}
