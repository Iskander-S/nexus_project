package service;

import java.nio.file.Path;

public interface AvatarStorage {
    String store(String userId, Path source);

    void delete(String storedPath);
}
