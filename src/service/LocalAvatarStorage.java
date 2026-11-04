package service;

import exception.AvatarException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

public final class LocalAvatarStorage implements AvatarStorage {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private final Path directory;

    public LocalAvatarStorage() {
        this(Path.of(System.getProperty("user.home"), ".nexus", "avatars"));
    }

    public LocalAvatarStorage(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public Path directory() {
        return directory;
    }

    @Override
    public String store(String userId, Path source) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID must not be empty");
        }
        if (source == null || !Files.isRegularFile(source)) {
            throw new AvatarException("Image file not found.");
        }
        String extension = extensionOf(source);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AvatarException("Unsupported image type. Use PNG, JPG or JPEG.");
        }
        try {
            if (Files.size(source) > MAX_BYTES) {
                throw new AvatarException("Image is too large. The limit is " + MAX_BYTES / (1024 * 1024) + " MB.");
            }
            Files.createDirectories(directory);
            Path target = directory.resolve(safeName(userId) + "-" + System.currentTimeMillis() + "." + extension);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new AvatarException("Could not save the image: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        Path file = Path.of(storedPath).toAbsolutePath().normalize();
        if (!file.startsWith(directory)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private static String extensionOf(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String safeName(String userId) {
        return userId.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
