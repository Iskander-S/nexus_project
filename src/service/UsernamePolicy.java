package service;

import exception.AuthException;
import exception.AuthException.Reason;

import java.util.regex.Pattern;

public final class UsernamePolicy {
    private static final Pattern USERNAME = Pattern.compile("[\\p{L}\\p{N}_.-]{3,30}");

    private UsernamePolicy() {
    }

    public static String requireValid(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (!USERNAME.matcher(name).matches()) {
            throw new AuthException(Reason.INVALID_USERNAME,
                    "Username must be 3-30 characters: letters, digits, '_', '.', '-'");
        }
        return name;
    }
}
