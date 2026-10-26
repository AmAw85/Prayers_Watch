package com.horloge.islamique.data;

/** A checked failure from a remote or imported source. Never contains secrets. */
public final class DataSourceException extends Exception {
    public enum Code {
        NETWORK,
        HTTP,
        INVALID_DATA,
        TIMEZONE,
        STORAGE,
        NO_PROVIDER
    }

    private final Code code;

    public DataSourceException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public DataSourceException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public String codeKey() {
        switch (code) {
            case NETWORK:
                return "network";
            case HTTP:
                return "http";
            case INVALID_DATA:
                return "invalid_data";
            case TIMEZONE:
                return "timezone";
            case STORAGE:
                return "storage";
            case NO_PROVIDER:
            default:
                return "no_provider";
        }
    }
}
