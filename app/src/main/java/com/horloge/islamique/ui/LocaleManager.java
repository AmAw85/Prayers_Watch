package com.horloge.islamique.ui;

import android.content.Context;
import android.content.res.Configuration;

import com.horloge.islamique.settings.AppLanguage;

import java.util.Locale;

/** Applies the persisted language to an Activity context, including text direction. */
public final class LocaleManager {

    private LocaleManager() {
    }

    public static Context wrap(Context base, AppLanguage language) {
        Locale locale = language.locale();
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return base.createConfigurationContext(configuration);
    }

    public static boolean isRtl(AppLanguage language) {
        return language.isRtl();
    }
}
