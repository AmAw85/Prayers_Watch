package com.horloge.islamique.core;

/** Machine-readable validation findings. Localisation happens in the UI. */
public final class ScheduleIssue {
    public enum Code {
        MISSING_TIME,
        DUPLICATE_TIME,
        NON_CHRONOLOGICAL,
        INVALID_INTERVAL,
        OVERLAPPING_INTERVALS,
        INTERVALS_NOT_CONFIGURED,
        AMBIGUOUS_LOCAL_TIME,
        NONEXISTENT_LOCAL_TIME,
        HIJRI_MISSING,
        HIJRI_IMPLAUSIBLE,
        TIMEZONE_MISMATCH
    }

    private final Code code;
    private final String detail;

    public ScheduleIssue(Code code, String detail) {
        this.code = code;
        this.detail = detail == null ? "" : detail;
    }

    public Code code() {
        return code;
    }

    public String detail() {
        return detail;
    }

    public boolean isFatal() {
        return code == Code.MISSING_TIME
                || code == Code.DUPLICATE_TIME
                || code == Code.NON_CHRONOLOGICAL
                || code == Code.INVALID_INTERVAL
                || code == Code.OVERLAPPING_INTERVALS
                || code == Code.AMBIGUOUS_LOCAL_TIME
                || code == Code.NONEXISTENT_LOCAL_TIME
                || code == Code.TIMEZONE_MISMATCH;
    }
}
