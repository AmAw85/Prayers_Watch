package com.horloge.islamique.core;

import java.time.Instant;
import java.util.Objects;

/** A user correction layered over an immutable source value. */
public final class Correction {
    private final Prayer prayer;
    private final Instant original;
    private final Instant corrected;
    private final Instant correctedAt;
    private final String reason;
    private final String provenance;

    public Correction(Prayer prayer, Instant original, Instant corrected,
                      Instant correctedAt, String reason, String provenance) {
        this.prayer = Objects.requireNonNull(prayer, "prayer");
        this.original = Objects.requireNonNull(original, "original");
        this.corrected = Objects.requireNonNull(corrected, "corrected");
        this.correctedAt = Objects.requireNonNull(correctedAt, "correctedAt");
        this.reason = reason == null ? "" : reason;
        this.provenance = provenance == null ? "" : provenance;
    }

    public Prayer prayer() {
        return prayer;
    }

    public Instant original() {
        return original;
    }

    public Instant corrected() {
        return corrected;
    }

    public Instant correctedAt() {
        return correctedAt;
    }

    public String reason() {
        return reason;
    }

    public String provenance() {
        return provenance;
    }
}
