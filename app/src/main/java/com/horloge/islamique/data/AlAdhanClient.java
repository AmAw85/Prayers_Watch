package com.horloge.islamique.data;

import com.horloge.islamique.core.PrayerDay;
import com.horloge.islamique.core.SourceInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * AlAdhan is a calculated source, not an Algerian official publication.
 * Endpoint verified against https://api.aladhan.com/v1/documentation/openapi/prayer-times/yaml
 */
public final class AlAdhanClient implements PrayerSource {
    public static final String PROVIDER_ID = "aladhan";
    private static final String BASE_URL = "https://api.aladhan.com/v1/timings/";
    private static final int MAX_BYTES = 1_000_000;
    private final Clock clock;

    public AlAdhanClient() {
        this(Clock.systemUTC());
    }

    public AlAdhanClient(Clock clock) {
        this.clock = clock;
    }

    @Override
    public SourceInfo info() {
        return new SourceInfo(
                PROVIDER_ID,
                "AlAdhan — Algeria (calculated)",
                SourceInfo.Kind.CALCULATED,
                "AlAdhan.com / Islamic Network, calculation method 19 (Algeria), "
                        + "Fajr 18°, Isha 17°. Calculated, never official. "
                        + "https://aladhan.com/prayer-times-api"
        );
    }

    @Override
    public PrayerDay fetch(LocalDate date, City city, ZoneId zone) throws DataSourceException {
        String datePart = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT).format(date);
        String url = BASE_URL + datePart
                + "?latitude=" + city.latitude()
                + "&longitude=" + city.longitude()
                + "&method=19"
                + "&timezonestring=" + encode(zone.getId());
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "HorlogeIslamique/1.0 (Android)");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new DataSourceException(DataSourceException.Code.HTTP,
                        "HTTP status " + status);
            }
            String body = readLimited(connection.getInputStream());
            Instant fetchedAt = clock.instant();
            return AlAdhanParser.parse(body, date, zone, city.id(), info(), fetchedAt);
        } catch (DataSourceException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new DataSourceException(DataSourceException.Code.NETWORK,
                    "Network request failed", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            return value;
        }
    }

    private static String readLimited(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new IOException("Response too large");
                }
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
