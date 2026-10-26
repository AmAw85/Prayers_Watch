package com.horloge.islamique.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Small curated list for the MVP. It is not an exhaustive Algerian gazetteer. */
public final class City {
    private final String id;
    private final String nameAr;
    private final String nameFr;
    private final String nameEn;
    private final double latitude;
    private final double longitude;
    private final String zoneId;

    public City(String id, String nameAr, String nameFr, String nameEn,
                double latitude, double longitude, String zoneId) {
        this.id = id;
        this.nameAr = nameAr;
        this.nameFr = nameFr;
        this.nameEn = nameEn;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoneId = zoneId;
    }

    public String id() {
        return id;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public String zoneId() {
        return zoneId;
    }

    public String name(String language) {
        if ("fr".equals(language)) {
            return nameFr;
        }
        if ("en".equals(language)) {
            return nameEn;
        }
        return nameAr;
    }

    public static final List<City> ALGERIA = Collections.unmodifiableList(Arrays.asList(
            new City("algiers", "الجزائر", "Alger", "Algiers", 36.7538, 3.0588, "Africa/Algiers"),
            new City("oran", "وهران", "Oran", "Oran", 35.6971, -0.6308, "Africa/Algiers"),
            new City("constantine", "قسنطينة", "Constantine", "Constantine", 36.3650, 6.6147, "Africa/Algiers"),
            new City("annaba", "عنابة", "Annaba", "Annaba", 36.9000, 7.7667, "Africa/Algiers"),
            new City("setif", "سطيف", "Sétif", "Setif", 36.1911, 5.4137, "Africa/Algiers"),
            new City("batna", "باتنة", "Batna", "Batna", 35.5550, 6.1741, "Africa/Algiers"),
            new City("tlemcen", "تلمسان", "Tlemcen", "Tlemcen", 34.8828, -1.3167, "Africa/Algiers"),
            new City("ouargla", "ورقلة", "Ouargla", "Ouargla", 31.9500, 5.3167, "Africa/Algiers"),
            new City("ghardaia", "غرداية", "Ghardaïa", "Ghardaia", 32.4900, 3.6700, "Africa/Algiers"),
            new City("bechar", "بشار", "Béchar", "Bechar", 31.6167, -2.2167, "Africa/Algiers"),
            new City("tamanrasset", "تمنراست", "Tamanrasset", "Tamanrasset", 22.7850, 5.5228, "Africa/Algiers"),
            new City("adrar", "أدرار", "Adrar", "Adrar", 27.8743, -0.2939, "Africa/Algiers")
    ));

    public static City byId(String id) {
        for (City city : ALGERIA) {
            if (city.id.equals(id)) {
                return city;
            }
        }
        return ALGERIA.get(0);
    }
}
