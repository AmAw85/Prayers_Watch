package com.horloge.islamique.settings;

/** Individually customisable colours. Defaults are visual choices, not religious ones. */
public enum ColorSlot {
    FAJR("color_fajr", 0xFF315C9B, "fajr"),
    DHUHR("color_dhuhr", 0xFF2D8069, "dhuhr"),
    ASR("color_asr", 0xFF7956A8, "asr"),
    MAGHRIB("color_maghrib", 0xFFC87532, "maghrib"),
    ISHA("color_isha", 0xFF34495E, "isha"),
    OPTIONAL("color_optional", 0xFF70B89B, "optional"),
    NECESSARY("color_necessary", 0xFFD6A23D, "necessary");

    private final String preferenceKey;
    private final int defaultColor;
    private final String id;

    ColorSlot(String preferenceKey, int defaultColor, String id) {
        this.preferenceKey = preferenceKey;
        this.defaultColor = defaultColor;
        this.id = id;
    }

    public String preferenceKey() {
        return preferenceKey;
    }

    public int defaultColor() {
        return defaultColor;
    }

    public String id() {
        return id;
    }

    public static ColorSlot fromId(String id) {
        for (ColorSlot slot : values()) {
            if (slot.id.equals(id)) {
                return slot;
            }
        }
        return null;
    }
}
