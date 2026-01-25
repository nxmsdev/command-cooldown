package dev.nxms.commandcooldown.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Parsuje string czasowy do sekund.
     * Obsługuje formaty: "30", "30s", "5m", "1h30m", "1d12h30m45s"
     *
     * @param timeString String do sparsowania
     * @return Czas w sekundach
     */
    public static int parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0;
        }

        timeString = timeString.trim().toLowerCase();

        // Spróbuj sparsować jako liczbę (sekundy)
        try {
            return Integer.parseInt(timeString);
        } catch (NumberFormatException ignored) {
        }

        // Spróbuj sparsować jako format czasowy
        Matcher matcher = TIME_PATTERN.matcher(timeString);

        if (!matcher.matches()) {
            return 0;
        }

        int seconds = 0;

        // Dni
        if (matcher.group(1) != null) {
            seconds += Integer.parseInt(matcher.group(1)) * 86400;
        }

        // Godziny
        if (matcher.group(2) != null) {
            seconds += Integer.parseInt(matcher.group(2)) * 3600;
        }

        // Minuty
        if (matcher.group(3) != null) {
            seconds += Integer.parseInt(matcher.group(3)) * 60;
        }

        // Sekundy
        if (matcher.group(4) != null) {
            seconds += Integer.parseInt(matcher.group(4));
        }

        return seconds;
    }

    /**
     * Formatuje sekundy do czytelnego formatu.
     *
     * @param seconds Czas w sekundach
     * @return Sformatowany string
     */
    public static String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }

        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) sb.append(days).append("d");
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString();
    }

    /**
     * Formatuje sekundy do pełnego polskiego formatu.
     *
     * @param seconds Czas w sekundach
     * @return Sformatowany string po polsku
     */
    public static String formatTimeFull(long seconds) {
        if (seconds <= 0) {
            return "0 sekund";
        }

        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(" ").append(getPolishForm(days, "dzień", "dni", "dni"));
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(hours).append(" ").append(getPolishForm(hours, "godzina", "godziny", "godzin"));
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(minutes).append(" ").append(getPolishForm(minutes, "minuta", "minuty", "minut"));
        }
        if (seconds > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(seconds).append(" ").append(getPolishForm(seconds, "sekunda", "sekundy", "sekund"));
        }

        return sb.length() > 0 ? sb.toString() : "0 sekund";
    }

    /**
     * Zwraca odpowiednią formę słowa dla liczby po polsku.
     *
     * @param number   Liczba
     * @param singular Forma dla 1
     * @param plural2  Forma dla 2-4
     * @param plural5  Forma dla 5+
     * @return Odpowiednia forma słowa
     */
    private static String getPolishForm(long number, String singular, String plural2, String plural5) {
        if (number == 1) {
            return singular;
        }

        long lastTwo = number % 100;
        long lastOne = number % 10;

        if (lastTwo >= 12 && lastTwo <= 14) {
            return plural5;
        }

        if (lastOne >= 2 && lastOne <= 4) {
            return plural2;
        }

        return plural5;
    }
}