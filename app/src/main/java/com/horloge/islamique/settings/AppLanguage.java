package com.horloge.islamique.settings;

import java.util.Locale;

/** Supported application languages. Arabic is the default. */
public enum AppLanguage {
    ARABIC("ar", "العربية"),
    FRENCH("fr", "Français"),
    ENGLISH("en", "English");

    private final String code;
    private final String nativeName;

    AppLanguage(String code, String nativeName) {
        this.code = code;
        this.nativeName = nativeName;
    }

    public String code() {
        return code;
    }

    public String nativeName() {
        return nativeName;
    }

    public Locale locale() {
        return "ar".equals(code) ? new Locale("ar") : Locale.forLanguageTag(code);
    }

    public boolean isRtl() {
        return "ar".equals(code);
    }

    public static AppLanguage fromCode(String code) {
        if (code != null) {
            for (AppLanguage language : values()) {
                if (language.code.equalsIgnoreCase(code)) {
                    return language;
                }
            }
        }
        return ARABIC;
    }
}
