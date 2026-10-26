package com.horloge.islamique.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.horloge.islamique.settings.AppLanguage;
import com.horloge.islamique.settings.SettingsRepository;
import com.horloge.islamique.settings.SharedPreferencesStore;

import java.util.Locale;

/** Base activity that applies and re-applies the persisted language. */
public abstract class BaseActivity extends Activity {
    protected SettingsRepository settings;
    private String createdLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences preferences = newBase.getSharedPreferences(
                SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE);
        AppLanguage language = new SettingsRepository(
                new SharedPreferencesStore(preferences)).language();
        super.attachBaseContext(LocaleManager.wrap(newBase, language));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(
                SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE);
        settings = new SettingsRepository(new SharedPreferencesStore(preferences));
        createdLanguage = settings.language().code();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (settings != null
                && !createdLanguage.equals(settings.language().code())) {
            recreate();
        }
    }

    protected AppLanguage language() {
        return settings.language();
    }

    protected Locale locale() {
        return language().locale();
    }

    protected int color(int resourceId) {
        return getColor(resourceId);
    }
}
