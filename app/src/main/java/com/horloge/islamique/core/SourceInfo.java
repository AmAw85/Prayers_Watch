package com.horloge.islamique.core;

import java.util.Objects;

/** Identifies who produced a value and how much authority it has. */
public final class SourceInfo {
    public enum Kind {
        /** Published by a recognised authority and imported without alteration. */
        OFFICIAL,
        /** Computed from an astronomical/calculation convention. Not official. */
        CALCULATED,
        /** Approximate or fallback data, explicitly marked as such. */
        ESTIMATED,
        /** Entered or imported by the user. */
        MANUAL,
        /** Imported data whose authority has not been established. */
        UNVERIFIED
    }

    private final String id;
    private final String displayName;
    private final Kind kind;
    private final String attribution;

    public SourceInfo(String id, String displayName, Kind kind, String attribution) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName == null ? id : displayName;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.attribution = attribution == null ? "" : attribution;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Kind kind() {
        return kind;
    }

    public String attribution() {
        return attribution;
    }

    public static SourceInfo manual(String provenance) {
        return new SourceInfo("manual", "Saisie locale", Kind.MANUAL, provenance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SourceInfo)) {
            return false;
        }
        SourceInfo that = (SourceInfo) o;
        return id.equals(that.id)
                && displayName.equals(that.displayName)
                && kind == that.kind
                && attribution.equals(that.attribution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, kind, attribution);
    }
}
