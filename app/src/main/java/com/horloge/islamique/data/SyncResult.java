package com.horloge.islamique.data;

import java.time.Instant;

/** Outcome of a manual or automatic synchronisation. */
public final class SyncResult {
    public final boolean success;
    public final int savedDays;
    public final Instant at;
    public final String errorCode;

    private SyncResult(boolean success, int savedDays, Instant at, String errorCode) {
        this.success = success;
        this.savedDays = savedDays;
        this.at = at;
        this.errorCode = errorCode;
    }

    public static SyncResult success(int savedDays, Instant at) {
        return new SyncResult(true, savedDays, at, "");
    }

    public static SyncResult failure(String errorCode, Instant at, int savedDays) {
        return new SyncResult(false, savedDays, at, errorCode);
    }
}
