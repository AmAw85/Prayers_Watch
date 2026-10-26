package com.horloge.islamique.core;

import java.time.Instant;
import java.util.Objects;

/** A semi-open interval [start, end), always stored as absolute instants. */
public final class TimeInterval {
    private final String id;
    private final String label;
    private final Instant start;
    private final Instant end;
    private final String provenance;

    public TimeInterval(String id, String label, Instant start, Instant end, String provenance) {
        this.id = id == null ? "" : id;
        this.label = label == null ? "" : label;
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
        this.provenance = provenance == null ? "" : provenance;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public Instant start() {
        return start;
    }

    public Instant end() {
        return end;
    }

    public String provenance() {
        return provenance;
    }

    public boolean hasValidRange() {
        return start.isBefore(end);
    }

    public boolean contains(Instant instant) {
        return hasValidRange()
                && !instant.isBefore(start)
                && instant.isBefore(end);
    }

    public boolean overlaps(TimeInterval other) {
        return hasValidRange()
                && other.hasValidRange()
                && start.isBefore(other.end)
                && other.start.isBefore(end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimeInterval)) {
            return false;
        }
        TimeInterval that = (TimeInterval) o;
        return id.equals(that.id)
                && label.equals(that.label)
                && start.equals(that.start)
                && end.equals(that.end)
                && provenance.equals(that.provenance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, start, end, provenance);
    }
}
