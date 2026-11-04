package service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        Base64.Encoder enc = Base64.getEncoder();
        return enc.encodeToString(salt) + "$" + enc.encodeToString(digest(salt, password));
    }

    public static boolean matches(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        int separator = stored.indexOf('$');
        if (separator <= 0) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(stored.substring(0, separator));
            byte[] expected = Base64.getDecoder().decode(stored.substring(separator + 1));
            return MessageDigest.isEqual(expected, digest(salt, password));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] digest(byte[] salt, String password) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(salt);
            return sha.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
