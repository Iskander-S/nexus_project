import exception.AuthException;
import exception.AuthException.Reason;
import model.User;
import org.junit.jupiter.api.Test;
import service.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {

    TaskServiceTest.MemUsers users = new TaskServiceTest.MemUsers();
    UserService service = new UserService(users);

    private User withXp(String id, String name, int xp) {
        User user = new User(id, name);
        user.addXp(xp);
        users.save(user);
        return user;
    }

    @Test
    void leaderboardIsSortedByXpDescendingAndLimited() {
        for (int i = 1; i <= 12; i++) {
            withXp("u" + i, "Player" + String.format("%02d", i), i * 10);
        }

        List<User> top = service.getLeaderboard();

        assertThat(top.size()).isEqualTo(10);
        assertThat(top.get(0).getName()).isEqualTo("Player12");
        assertThat(top.get(9).getName()).isEqualTo("Player03");
    }

    @Test
    void leaderboardRejectsNonPositiveLimit() {
        assertThatThrownBy(() -> service.getLeaderboard(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renameChangesNameAndPersistsIt() {
        User user = withXp("u1", "Alice", 50);

        service.renameUser(user, "  Alicia  ");

        assertThat(user.getName()).isEqualTo("Alicia");
        assertThat(users.data.get("u1").getName()).isEqualTo("Alicia");
        assertThat(users.data.get("u1").getCurrentXp()).isEqualTo(50);
    }

    @Test
    void renameAllowsChangingOnlyTheCaseOfOwnName() {
        User user = withXp("u1", "alice", 0);

        service.renameUser(user, "ALICE");

        assertThat(user.getName()).isEqualTo("ALICE");
    }

    @Test
    void renameToTakenNameIsRejected() {
        withXp("u2", "Bob", 0);
        User user = withXp("u1", "Alice", 0);

        try {
            service.renameUser(user, "bob");
            throw new AssertionError("AuthException expected");
        } catch (AuthException e) {
            assertThat(e.getReason()).isEqualTo(Reason.USER_EXISTS);
        }
        assertThat(user.getName()).isEqualTo("Alice");
    }

    @Test
    void renameToInvalidNameIsRejected() {
        User user = withXp("u1", "Alice", 0);

        try {
            service.renameUser(user, "a b");
            throw new AssertionError("AuthException expected");
        } catch (AuthException e) {
            assertThat(e.getReason()).isEqualTo(Reason.INVALID_USERNAME);
        }
        assertThat(user.getName()).isEqualTo("Alice");
    }

    @Test
    void newUserHasNoAvatar() {
        User user = new User("u1", "Alice");

        assertThat(user.hasAvatar()).isFalse();
        assertThat(user.getAvatarPath()).isNull();
    }

    @Test
    void renameKeepsAvatarPath() {
        User user = new User("u1", "Alice", 1, 0, model.Rank.E_RANK, "/tmp/a.png");
        users.save(user);

        service.renameUser(user, "Alicia");

        assertThat(users.data.get("u1").getAvatarPath()).isEqualTo("/tmp/a.png");
    }
}
