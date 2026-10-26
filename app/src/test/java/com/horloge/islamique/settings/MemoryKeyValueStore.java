package com.horloge.islamique.settings;

import java.util.HashMap;
import java.util.Map;

/** In-memory KeyValueStore used by JVM unit tests. */
public final class MemoryKeyValueStore implements KeyValueStore {
    private final Map<String, Object> values = new HashMap<>();

    @Override
    public String getString(String key, String fallback) {
        Object value = values.get(key);
        return value instanceof String ? (String) value : fallback;
    }

    @Override
    public int getInt(String key, int fallback) {
        Object value = values.get(key);
        return value instanceof Integer ? (Integer) value : fallback;
    }

    @Override
    public long getLong(String key, long fallback) {
        Object value = values.get(key);
        return value instanceof Long ? (Long) value : fallback;
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    @Override
    public void putString(String key, String value) {
        values.put(key, value);
    }

    @Override
    public void putInt(String key, int value) {
        values.put(key, value);
    }

    @Override
    public void putLong(String key, long value) {
        values.put(key, value);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        values.put(key, value);
    }

    @Override
    public void remove(String key) {
        values.remove(key);
    }
}
