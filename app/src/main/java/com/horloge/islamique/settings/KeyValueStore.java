package com.horloge.islamique.settings;

/** Small storage abstraction so settings can be tested without Android. */
public interface KeyValueStore {
    String getString(String key, String fallback);

    int getInt(String key, int fallback);

    long getLong(String key, long fallback);

    boolean getBoolean(String key, boolean fallback);

    void putString(String key, String value);

    void putInt(String key, int value);

    void putLong(String key, long value);

    void putBoolean(String key, boolean value);

    void remove(String key);
}
