package ru.kursach.kpo.tour_agency_backend.core.configuration;

public class Constants {
    public static final String API_URL = "/api";

    public static final String ADMIN_PREFIX = "/admin";

    public static final String LOGIN_URL = "/login";

    public  static final String AUTH_URL = "/auth";

    public static final String DEFAULT_PAGE_SIZE = "4";

    public static final Integer OTP_EXPIRATION_MINUTES = 10;

    public static final String PHONE_PATTERN = "^((8|\\+374|\\+994|\\+995|\\+375|\\+7|\\+380|\\+38|\\+996|\\+998|\\+993)[\\- ]?)?\\(?\\d{3,5}\\)?[\\- ]?\\d{1}[\\- ]?\\d{1}[\\- ]?\\d{1}[\\- ]?\\d{1}[\\- ]?\\d{1}(([\\- ]?\\d{1})?[\\- ]?\\d{1})?$";

    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*_=+\\\\-]).{8,60}$";

    public static final String LOG_PATH = "logs";
    private Constants() {
    }
}
