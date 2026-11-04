import exception.AvatarException;
import org.junit.jupiter.api.Test;
import service.LocalAvatarStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalAvatarStorageTest {

    private Path tempDir() throws IOException {
        return Files.createTempDirectory("nexus-avatars-test");
    }

    private Path image(Path dir, String name, int bytes) throws IOException {
        Path file = dir.resolve(name);
        Files.write(file, new byte[bytes]);
        return file;
    }

    @Test
    void copiesImageIntoAvatarDirectory() throws IOException {
        Path root = tempDir();
        LocalAvatarStorage storage = new LocalAvatarStorage(root.resolve("avatars"));
        Path source = image(root, "me.PNG", 10);

        String stored = storage.store("user_1", source);

        assertThat(Files.exists(Path.of(stored))).isTrue();
        assertThat(Path.of(stored).startsWith(storage.directory())).isTrue();
        assertThat(stored.endsWith(".png")).isTrue();
        assertThat(Files.exists(source)).isTrue();
    }

    @Test
    void acceptsJpgAndJpeg() throws IOException {
        Path root = tempDir();
        LocalAvatarStorage storage = new LocalAvatarStorage(root.resolve("avatars"));

        assertThat(storage.store("u1", image(root, "a.jpg", 5)).endsWith(".jpg")).isTrue();
        assertThat(storage.store("u2", image(root, "b.jpeg", 5)).endsWith(".jpeg")).isTrue();
    }

    @Test
    void rejectsUnsupportedExtension() throws IOException {
        Path root = tempDir();
        LocalAvatarStorage storage = new LocalAvatarStorage(root.resolve("avatars"));
        Path gif = image(root, "a.gif", 5);

        assertThatThrownBy(() -> storage.store("u1", gif)).isInstanceOf(AvatarException.class);
    }

    @Test
    void rejectsMissingFile() throws IOException {
        Path root = tempDir();
        LocalAvatarStorage storage = new LocalAvatarStorage(root.resolve("avatars"));

        assertThatThrownBy(() -> storage.store("u1", root.resolve("missing.png"))).isInstanceOf(AvatarException.class);
    }

    @Test
    void rejectsTooLargeFile() throws IOException {
        Path root = tempDir();
        LocalAvatarStorage storage = new LocalAvatarStorage(root.resolve("avatars"));
        Path big = image(root, "big.png", (int) LocalAvatarStorage.MAX_BYTES + 1);

        assertThatThrownBy(() -> storage.store("u1", big)).isInstanceOf(AvatarException.class);
    }

    @Test
    void deleteRemovesOnlyFilesInsideAvatarDirectory() throws IOException {
        Path root = tempDir();
        LocalAvatarStorage storage = new LocalAvatarStorage(root.resolve("avatars"));
        Path outside = image(root, "outside.png", 5);
        String stored = storage.store("u1", outside);

        storage.delete(outside.toString());
        storage.delete(stored);
        storage.delete(null);

        assertThat(Files.exists(outside)).isTrue();
        assertThat(Files.exists(Path.of(stored))).isFalse();
    }

    @Test
    void storedFilesOfDifferentUsersDoNotCollide() throws IOException {
        Path root = tempDir();
        LocalAvatarStorage storage = new LocalAvatarStorage(root.resolve("avatars"));
        Path source = image(root, "a.png", 5);

        String first = storage.store("u1", source);
        String second = storage.store("u2", source);

        assertThat(first).isNotEqualTo(second);
    }
}
