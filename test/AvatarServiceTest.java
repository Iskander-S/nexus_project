import db.UserRepository;
import exception.DataAccessException;
import model.User;
import org.junit.jupiter.api.Test;
import service.AvatarService;
import service.AvatarStorage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarServiceTest {

    static class MemStorage implements AvatarStorage {
        List<String> stored = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        int counter;

        public String store(String userId, Path source) {
            String path = "/avatars/" + userId + "-" + (++counter) + ".png";
            stored.add(path);
            return path;
        }

        public void delete(String storedPath) {
            if (storedPath != null) {
                deleted.add(storedPath);
            }
        }
    }

    TaskServiceTest.MemUsers users = new TaskServiceTest.MemUsers();
    MemStorage storage = new MemStorage();
    AvatarService service = new AvatarService(users, storage);

    private User saved(String id) {
        User user = new User(id, "Tester");
        user.addXp(120);
        users.save(user);
        return user;
    }

    @Test
    void changeAvatarStoresFileAndPersistsPath() {
        User user = saved("u1");

        service.changeAvatar(user, Path.of("photo.png"));

        assertThat(user.getAvatarPath()).isEqualTo("/avatars/u1-1.png");
        assertThat(users.data.get("u1").getAvatarPath()).isEqualTo("/avatars/u1-1.png");
        assertThat(users.data.get("u1").getCurrentXp()).isEqualTo(120);
        assertThat(user.hasAvatar()).isTrue();
    }

    @Test
    void replacingAvatarDeletesThePreviousFile() {
        User user = saved("u1");
        service.changeAvatar(user, Path.of("a.png"));

        service.changeAvatar(user, Path.of("b.png"));

        assertThat(user.getAvatarPath()).isEqualTo("/avatars/u1-2.png");
        assertThat(storage.deleted.contains("/avatars/u1-1.png")).isTrue();
    }

    @Test
    void failedSaveRemovesNewFileAndKeepsOldAvatar() {
        User user = saved("u1");
        service.changeAvatar(user, Path.of("a.png"));
        users.failOnSave = true;

        assertThatThrownBy(() -> service.changeAvatar(user, Path.of("b.png"))).isInstanceOf(DataAccessException.class);

        assertThat(user.getAvatarPath()).isEqualTo("/avatars/u1-1.png");
        assertThat(storage.deleted.contains("/avatars/u1-2.png")).isTrue();
        assertThat(storage.deleted.contains("/avatars/u1-1.png")).isFalse();
    }

    @Test
    void removeAvatarClearsPathAndDeletesFile() {
        User user = saved("u1");
        service.changeAvatar(user, Path.of("a.png"));

        service.removeAvatar(user);

        assertThat(user.hasAvatar()).isFalse();
        assertThat(users.data.get("u1").getAvatarPath()).isNull();
        assertThat(storage.deleted.contains("/avatars/u1-1.png")).isTrue();
    }

    @Test
    void removeAvatarWithoutAvatarDoesNothing() {
        User user = saved("u1");

        service.removeAvatar(user);

        assertThat(storage.deleted.isEmpty()).isTrue();
    }
}
