package com.horloge.islamique.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.horloge.islamique.core.Correction;
import com.horloge.islamique.core.HijriDate;
import com.horloge.islamique.core.Prayer;
import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.ScheduleIssue;
import com.horloge.islamique.core.SourceInfo;
import com.horloge.islamique.core.TimeInterval;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** SQLite persistence preserving source and corrected values in distinct columns. */
public final class PrayerDatabase extends SQLiteOpenHelper implements PrayerStore {
    private static final String DB_NAME = "horloge_islamique.db";
    private static final int DB_VERSION = 1;

    public PrayerDatabase(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE prayer_days ("
                + "date TEXT PRIMARY KEY,"
                + "zone_id TEXT NOT NULL,"
                + "location_id TEXT NOT NULL DEFAULT '',"
                + "source_id TEXT NOT NULL,"
                + "source_name TEXT NOT NULL,"
                + "source_kind TEXT NOT NULL,"
                + "attribution TEXT NOT NULL DEFAULT '',"
                + "fetched_at INTEGER NOT NULL,"
                + "hijri_json TEXT,"
                + "optional_json TEXT NOT NULL DEFAULT '[]',"
                + "necessary_json TEXT NOT NULL DEFAULT '[]',"
                + "issues_json TEXT NOT NULL DEFAULT '[]'"
                + ")");
        db.execSQL("CREATE TABLE prayer_times ("
                + "date TEXT NOT NULL,"
                + "prayer TEXT NOT NULL,"
                + "original_epoch INTEGER NOT NULL,"
                + "corrected_epoch INTEGER,"
                + "corrected_at INTEGER,"
                + "reason TEXT,"
                + "provenance TEXT,"
                + "PRIMARY KEY (date, prayer),"
                + "FOREIGN KEY (date) REFERENCES prayer_days(date) ON DELETE CASCADE"
                + ")");
        db.execSQL("CREATE INDEX idx_prayer_times_date ON prayer_times(date)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1: no migrations yet.
    }

    @Override
    public synchronized List<PrayerDay> loadRange(LocalDate from, LocalDate to) {
        List<PrayerDay> result = new ArrayList<>();
        if (from == null || to == null || to.isBefore(from)) {
            return result;
        }
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            PrayerDay day = load(cursor);
            if (day != null) {
                result.add(day);
            }
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    @Override
    public synchronized PrayerDay load(LocalDate date) {
        if (date == null) {
            return null;
        }
        SQLiteDatabase db = getReadableDatabase();
        String iso = date.toString();
        try (Cursor cursor = db.query("prayer_days", null, "date = ?",
                new String[]{iso}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            ZoneId zone;
            try {
                zone = ZoneId.of(cursor.getString(cursor.getColumnIndexOrThrow("zone_id")));
            } catch (RuntimeException ex) {
                zone = ZoneId.of("UTC");
            }
            String locationId = cursor.getString(cursor.getColumnIndexOrThrow("location_id"));
            SourceInfo source = new SourceInfo(
                    cursor.getString(cursor.getColumnIndexOrThrow("source_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("source_name")),
                    parseKind(cursor.getString(cursor.getColumnIndexOrThrow("source_kind"))),
                    cursor.getString(cursor.getColumnIndexOrThrow("attribution")));
            Instant fetchedAt = Instant.ofEpochMilli(
                    cursor.getLong(cursor.getColumnIndexOrThrow("fetched_at")));
            HijriDate hijri = parseHijri(
                    cursor.getString(cursor.getColumnIndexOrThrow("hijri_json")));
            List<TimeInterval> optional = parseIntervals(
                    cursor.getString(cursor.getColumnIndexOrThrow("optional_json")));
            List<TimeInterval> necessary = parseIntervals(
                    cursor.getString(cursor.getColumnIndexOrThrow("necessary_json")));
            List<ScheduleIssue> issues = parseIssues(
                    cursor.getString(cursor.getColumnIndexOrThrow("issues_json")));

            Map<Prayer, Instant> originals = new EnumMap<>(Prayer.class);
            Map<Prayer, Correction> corrections = new EnumMap<>(Prayer.class);
            try (Cursor times = db.query("prayer_times", null, "date = ?",
                    new String[]{iso}, null, null, null)) {
                while (times.moveToNext()) {
                    Prayer prayer = Prayer.fromKey(
                            times.getString(times.getColumnIndexOrThrow("prayer")));
                    if (prayer == null) {
                        continue;
                    }
                    Instant original = Instant.ofEpochMilli(
                            times.getLong(times.getColumnIndexOrThrow("original_epoch")));
                    originals.put(prayer, original);
                    int correctedIndex = times.getColumnIndexOrThrow("corrected_epoch");
                    if (!times.isNull(correctedIndex)) {
                        Instant corrected = Instant.ofEpochMilli(times.getLong(correctedIndex));
                        int correctedAtIndex = times.getColumnIndexOrThrow("corrected_at");
                        Instant correctedAt = times.isNull(correctedAtIndex)
                                ? fetchedAt
                                : Instant.ofEpochMilli(times.getLong(correctedAtIndex));
                        String reason = times.getString(
                                times.getColumnIndexOrThrow("reason"));
                        String provenance = times.getString(
                                times.getColumnIndexOrThrow("provenance"));
                        corrections.put(prayer, new Correction(
                                prayer, original, corrected, correctedAt, reason, provenance));
                    }
                }
            }
            return new PrayerDay(date, zone, locationId, source, fetchedAt, originals, corrections,
                    optional, necessary, hijri, issues);
        }
    }

    @Override
    public synchronized void saveProviderDay(PrayerDay day) {
        if (day == null) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Map<Prayer, Correction> existingCorrections = readCorrections(db, day.date());
            ContentValues dayValues = new ContentValues();
            dayValues.put("date", day.date().toString());
            dayValues.put("zone_id", day.zone().getId());
            dayValues.put("location_id", day.locationId());
            dayValues.put("source_id", day.source().id());
            dayValues.put("source_name", day.source().displayName());
            dayValues.put("source_kind", day.source().kind().name());
            dayValues.put("attribution", day.source().attribution());
            dayValues.put("fetched_at", day.fetchedAt() == null
                    ? Instant.now().toEpochMilli() : day.fetchedAt().toEpochMilli());
            dayValues.put("hijri_json", hijriJson(day.hijri()));
            dayValues.put("optional_json", intervalsJson(day.optionalIntervals()));
            dayValues.put("necessary_json", intervalsJson(day.necessaryIntervals()));
            dayValues.put("issues_json", issuesJson(day.issues()));
            int updated = db.update("prayer_days", dayValues, "date = ?",
                    new String[]{day.date().toString()});
            if (updated == 0) {
                db.insertOrThrow("prayer_days", null, dayValues);
            }

            for (Prayer prayer : Prayer.ALL) {
                Instant original = day.originalTimes().get(prayer);
                if (original == null) {
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put("date", day.date().toString());
                values.put("prayer", prayer.key());
                values.put("original_epoch", original.toEpochMilli());
                Correction correction = existingCorrections.get(prayer);
                if (correction != null) {
                    // Source value is refreshed; the pinned correction remains untouched.
                    values.put("corrected_epoch", correction.corrected().toEpochMilli());
                    values.put("corrected_at", correction.correctedAt().toEpochMilli());
                    values.put("reason", correction.reason());
                    values.put("provenance", correction.provenance());
                } else {
                    values.putNull("corrected_epoch");
                    values.putNull("corrected_at");
                    values.putNull("reason");
                    values.putNull("provenance");
                }
                db.insertWithOnConflict("prayer_times", null, values,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public synchronized void saveCorrection(LocalDate date, Prayer prayer, Instant original,
                                            Instant corrected, Instant correctedAt,
                                            String reason, String provenance) {
        ContentValues values = new ContentValues();
        values.put("corrected_epoch", corrected.toEpochMilli());
        values.put("corrected_at", correctedAt.toEpochMilli());
        values.put("reason", reason == null ? "" : reason);
        values.put("provenance", provenance == null ? "" : provenance);
        int updated = getWritableDatabase().update("prayer_times", values,
                "date = ? AND prayer = ?",
                new String[]{date.toString(), prayer.key()});
        if (updated == 0) {
            throw new IllegalStateException("No source time to correct");
        }
    }

    @Override
    public synchronized void clearCorrection(LocalDate date, Prayer prayer) {
        ContentValues values = new ContentValues();
        values.putNull("corrected_epoch");
        values.putNull("corrected_at");
        values.putNull("reason");
        values.putNull("provenance");
        getWritableDatabase().update("prayer_times", values,
                "date = ? AND prayer = ?",
                new String[]{date.toString(), prayer.key()});
    }

    @Override
    public synchronized LocalDate latestStoredDate() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT MAX(date) AS max_date FROM prayer_days", null)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return LocalDate.parse(cursor.getString(0));
            }
        }
        return null;
    }

    private Map<Prayer, Correction> readCorrections(SQLiteDatabase db, LocalDate date) {
        Map<Prayer, Correction> result = new EnumMap<>(Prayer.class);
        try (Cursor cursor = db.query("prayer_times", null,
                "date = ? AND corrected_epoch IS NOT NULL",
                new String[]{date.toString()}, null, null, null)) {
            while (cursor.moveToNext()) {
                Prayer prayer = Prayer.fromKey(
                        cursor.getString(cursor.getColumnIndexOrThrow("prayer")));
                if (prayer == null) {
                    continue;
                }
                Instant original = Instant.ofEpochMilli(
                        cursor.getLong(cursor.getColumnIndexOrThrow("original_epoch")));
                Instant corrected = Instant.ofEpochMilli(
                        cursor.getLong(cursor.getColumnIndexOrThrow("corrected_epoch")));
                Instant at = Instant.ofEpochMilli(
                        cursor.getLong(cursor.getColumnIndexOrThrow("corrected_at")));
                result.put(prayer, new Correction(
                        prayer, original, corrected, at,
                        cursor.getString(cursor.getColumnIndexOrThrow("reason")),
                        cursor.getString(cursor.getColumnIndexOrThrow("provenance"))));
            }
        }
        return result;
    }

    private static SourceInfo.Kind parseKind(String raw) {
        try {
            return SourceInfo.Kind.valueOf(raw);
        } catch (RuntimeException ex) {
            return SourceInfo.Kind.UNVERIFIED;
        }
    }

    private static String hijriJson(HijriDate hijri) {
        if (hijri == null) {
            return null;
        }
        try {
            JSONObject object = new JSONObject();
            object.put("day", hijri.day());
            object.put("month", hijri.month());
            object.put("year", hijri.year());
            object.put("month_ar", hijri.monthNameAr());
            object.put("month_en", hijri.monthNameEn());
            object.put("holidays", new JSONArray(hijri.holidays()));
            return object.toString();
        } catch (JSONException ex) {
            return null;
        }
    }

    private static HijriDate parseHijri(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(raw);
            List<String> holidays = new ArrayList<>();
            JSONArray array = object.optJSONArray("holidays");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    holidays.add(array.optString(i, ""));
                }
            }
            return new HijriDate(object.optInt("day"), object.optInt("month"),
                    object.optInt("year"), object.optString("month_ar"),
                    object.optString("month_en"), holidays);
        } catch (JSONException ex) {
            return null;
        }
    }

    private static String intervalsJson(List<TimeInterval> intervals) {
        JSONArray array = new JSONArray();
        try {
            for (TimeInterval interval : intervals) {
                JSONObject object = new JSONObject();
                object.put("id", interval.id());
                object.put("label", interval.label());
                object.put("start", interval.start().toEpochMilli());
                object.put("end", interval.end().toEpochMilli());
                object.put("provenance", interval.provenance());
                array.put(object);
            }
        } catch (JSONException ex) {
            return "[]";
        }
        return array.toString();
    }

    private static List<TimeInterval> parseIntervals(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<TimeInterval> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                result.add(new TimeInterval(
                        object.optString("id", ""),
                        object.optString("label", ""),
                        Instant.ofEpochMilli(object.getLong("start")),
                        Instant.ofEpochMilli(object.getLong("end")),
                        object.optString("provenance", "")));
            }
        } catch (JSONException ex) {
            return Collections.emptyList();
        }
        return result;
    }

    private static String issuesJson(List<ScheduleIssue> issues) {
        JSONArray array = new JSONArray();
        try {
            for (ScheduleIssue issue : issues) {
                JSONObject object = new JSONObject();
                object.put("code", issue.code().name());
                object.put("detail", issue.detail());
                array.put(object);
            }
        } catch (JSONException ex) {
            return "[]";
        }
        return array.toString();
    }

    private static List<ScheduleIssue> parseIssues(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<ScheduleIssue> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                try {
                    result.add(new ScheduleIssue(
                            ScheduleIssue.Code.valueOf(object.optString("code")),
                            object.optString("detail")));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown future codes rather than crashing on stored data.
                }
            }
        } catch (JSONException ex) {
            return Collections.emptyList();
        }
        return result;
    }
}
