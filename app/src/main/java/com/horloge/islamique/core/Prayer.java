package com.horloge.islamique.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The five daily prayers used by the application. */
public enum Prayer {
    FAJR("fajr"),
    DHUHR("dhuhr"),
    ASR("asr"),
    MAGHRIB("maghrib"),
    ISHA("isha");

    public static final List<Prayer> ALL =
            Collections.unmodifiableList(Arrays.asList(values()));

    private final String key;

    Prayer(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Prayer fromKey(String key) {
        for (Prayer prayer : values()) {
            if (prayer.key.equalsIgnoreCase(key)) {
                return prayer;
            }
        }
        return null;
    }
}
