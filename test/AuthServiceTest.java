import db.TransactionRunner;
import exception.AuthException;
import exception.AuthException.Reason;
import exception.DataAccessException;
import model.Task;
import model.User;
import org.junit.jupiter.api.Test;
import service.AuthService;
import service.EvaluationService;
import service.PasswordHasher;
import service.TaskService;
import service.UserService;

import java.sql.Connection;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {

    TaskServiceTest.MemUsers users = new TaskServiceTest.MemUsers();
    TaskServiceTest.MemTasks tasks = new TaskServiceTest.MemTasks();

    TransactionRunner tx = work -> {
        Map<String, User> usersBefore = new HashMap<>(users.data);
        Map<String, String> hashesBefore = new HashMap<>(users.hashes);
        Map<String, Task> tasksBefore = new LinkedHashMap<>(tasks.data);
        Map<String, String> ownersBefore = new HashMap<>(tasks.owner);
        try {
            work.execute(null);
        } catch (RuntimeException e) {
            users.data = usersBefore; users.hashes = hashesBefore;
            tasks.data = tasksBefore; tasks.owner = ownersBefore;
            throw e;
        } catch (java.sql.SQLException e) {
            throw new DataAccessException("tx", e);
        }
    };

    AuthService auth() {
        TaskService taskService = new TaskService(tasks, new UserService(users), new EvaluationService(), tx);
        return new AuthService(users, taskService, tx);
    }

    private static Reason reasonOf(Runnable action) {
        try {
            action.run();
        } catch (AuthException e) {
            return e.getReason();
        }
        throw new AssertionError("AuthException expected");
    }

    @Test
    void registerCreatesUserPasswordHashAndStarterQuests() {
        User user = auth().register("Alice", "secret123");

        assertThat(users.data.containsKey(user.getId())).isTrue();
        assertThat(users.hashes.get(user.getId())).isNotEqualTo("secret123");
        assertThat(tasks.data.size()).isEqualTo(4);
        assertThat(tasks.owner.values().stream().allMatch(user.getId()::equals)).isTrue();
    }

    @Test
    void starterQuestsAreActiveAndHavePositiveXp() {
        User user = auth().register("Alice", "secret123");
        TaskService service = new TaskService(tasks, new UserService(users), new EvaluationService(), tx);

        assertThat(service.getActiveTasks(user.getId()).size()).isEqualTo(4);
        assertThat(service.getActiveTasks(user.getId()).stream().allMatch(t -> t.getBaseXp() > 0)).isTrue();
    }

    @Test
    void duplicateUsernameIsRejectedIgnoringCase() {
        AuthService auth = auth();
        auth.register("Alice", "secret123");

        assertThat(reasonOf(() -> auth.register("ALICE", "another123"))).isEqualTo(Reason.USER_EXISTS);
        assertThat(users.data.size()).isEqualTo(1);
        assertThat(tasks.data.size()).isEqualTo(4);
    }

    @Test
    void invalidUsernameAndWeakPasswordAreRejectedWithoutSideEffects() {
        AuthService auth = auth();

        assertThat(reasonOf(() -> auth.register("a b", "secret123"))).isEqualTo(Reason.INVALID_USERNAME);
        assertThat(reasonOf(() -> auth.register("ab", "secret123"))).isEqualTo(Reason.INVALID_USERNAME);
        assertThat(reasonOf(() -> auth.register(null, "secret123"))).isEqualTo(Reason.INVALID_USERNAME);
        assertThat(reasonOf(() -> auth.register("alice", "12345"))).isEqualTo(Reason.WEAK_PASSWORD);
        assertThat(reasonOf(() -> auth.register("alice", null))).isEqualTo(Reason.WEAK_PASSWORD);
        assertThat(users.data.isEmpty()).isTrue();
        assertThat(tasks.data.isEmpty()).isTrue();
    }

    @Test
    void cyrillicUsernameIsAllowed() {
        assertThat(auth().register("Иван_2025", "secret123").getName()).isEqualTo("Иван_2025");
    }

    @Test
    void loginWithCorrectPasswordReturnsSameUser() {
        AuthService auth = auth();
        User registered = auth.register("Alice", "secret123");

        assertThat(auth.login("alice", "secret123").getId()).isEqualTo(registered.getId());
        assertThat(auth.login("  Alice  ", "secret123").getId()).isEqualTo(registered.getId());
    }

    @Test
    void loginFailsForWrongPasswordAndUnknownUser() {
        AuthService auth = auth();
        auth.register("Alice", "secret123");

        assertThat(reasonOf(() -> auth.login("Alice", "wrong-pass"))).isEqualTo(Reason.WRONG_PASSWORD);
        assertThat(reasonOf(() -> auth.login("Alice", null))).isEqualTo(Reason.WRONG_PASSWORD);
        assertThat(reasonOf(() -> auth.login("Bob", "secret123"))).isEqualTo(Reason.USER_NOT_FOUND);
    }

    @Test
    void legacyAccountWithoutPasswordCannotLogIn() {
        users.save(new User("legacy-id", "Iskander"));

        assertThat(reasonOf(() -> auth().login("Iskander", "anything"))).isEqualTo(Reason.NO_PASSWORD_SET);
    }

    @Test
    void failedStarterQuestInsertRollsBackTheWholeRegistration() {
        tasks = new TaskServiceTest.MemTasks() {
            @Override public void insertTask(Connection c, Task t, String userId) {
                throw new DataAccessException("boom");
            }
        };

        try {
            auth().register("Alice", "secret123");
            throw new AssertionError("expected DataAccessException");
        } catch (DataAccessException expected) {
        }
        assertThat(users.data.isEmpty()).isTrue();
        assertThat(users.hashes.isEmpty()).isTrue();
    }

    @Test
    void passwordHashIsSaltedAndVerifiable() {
        String first = PasswordHasher.hash("secret123");
        String second = PasswordHasher.hash("secret123");

        assertThat(first).isNotEqualTo(second);
        assertThat(PasswordHasher.matches("secret123", first)).isTrue();
        assertThat(PasswordHasher.matches("Secret123", first)).isFalse();
        assertThat(PasswordHasher.matches("secret123", "garbage")).isFalse();
        assertThat(PasswordHasher.matches("secret123", null)).isFalse();
    }
}
