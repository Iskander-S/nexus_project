import db.TaskRepository;
import db.TransactionRunner;
import db.UserRepository;
import exception.DataAccessException;
import model.DailyTask;
import model.GlobalQuest;
import model.Task;
import model.User;
import model.WeeklyTask;
import org.junit.jupiter.api.Test;
import service.EvaluationService;
import service.TaskService;
import service.UserService;

import java.sql.Connection;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskServiceTest {
    static class MemUsers implements UserRepository {
        Map<String, User> data = new HashMap<>();
        boolean failOnSave;

        public User save(User u) { return save(null, u); }
        public User save(Connection c, User u) {
            if (failOnSave) throw new DataAccessException("boom");
            data.put(u.getId(), new User(u.getId(), u.getName(), u.getLevel(), u.getCurrentXp(), u.getRank()));
            return u;
        }
        public Optional<User> findById(String id) { return Optional.ofNullable(data.get(id)); }
        public Optional<User> findByName(String n) { return data.values().stream().filter(u -> u.getName().equals(n)).findFirst(); }
        public List<User> findAll() { return new ArrayList<>(data.values()); }
        public boolean deleteById(String id) { return data.remove(id) != null; }
    }

    static class MemTasks implements TaskRepository {
        Map<String, Task> data = new LinkedHashMap<>();
        Map<String, String> owner = new HashMap<>();

        public void insertTask(Task t, String userId) {
            if (data.containsKey(t.getId())) throw new DataAccessException("Task with ID already exists: " + t.getId());
            data.put(t.getId(), copy(t)); owner.put(t.getId(), userId);
        }
        public void updateTask(Connection c, Task t, String userId) {
            if (!userId.equals(owner.get(t.getId()))) throw new DataAccessException("Task not found for this user");
            data.put(t.getId(), copy(t));
        }
        public List<Task> findByUserId(String userId) {
            return data.values().stream().filter(t -> owner.get(t.getId()).equals(userId)).map(MemTasks::copy).toList();
        }
        public Optional<Task> findById(String id) { return Optional.ofNullable(data.get(id)).map(MemTasks::copy); }
        public List<Task> findAll() { return new ArrayList<>(data.values()); }
        public boolean deleteById(String id) { return data.remove(id) != null; }

        static Task copy(Task t) {
            Task r;
            if (t instanceof GlobalQuest g) {
                r = new GlobalQuest(g.getId(), g.getTitle(), g.getDescription(), g.getBaseXp(), g.getTargetScore());
            } else if (t instanceof WeeklyTask) {
                r = new WeeklyTask(t.getId(), t.getTitle(), t.getDescription(), t.getBaseXp());
            } else {
                r = new DailyTask(t.getId(), t.getTitle(), t.getDescription(), t.getBaseXp());
            }
            r.setCompleted(t.isCompleted());
            return r;
        }
    }

    MemUsers users = new MemUsers();
    MemTasks tasks = new MemTasks();

    TransactionRunner rollbackAware = work -> {
        Map<String, Task> tasksBefore = new LinkedHashMap<>();
        tasks.data.forEach((k, v) -> tasksBefore.put(k, MemTasks.copy(v)));
        Map<String, User> usersBefore = new HashMap<>(users.data);
        try {
            work.execute(null);
        } catch (RuntimeException e) {
            tasks.data = tasksBefore;
            users.data = usersBefore;
            throw e;
        } catch (java.sql.SQLException e) {
            throw new DataAccessException("tx", e);
        }
    };

    TaskService service() {
        return new TaskService(tasks, new UserService(users), new EvaluationService(), rollbackAware);
    }

    User newUser() {
        User u = new User("u1", "Tester");
        users.save(u);
        return u;
    }

    @Test
    void completingDailyQuestAwardsBaseXpAndPersists() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "Run", "d", 120), user.getId());

        int xp = service.completeTask("T-1", user);

        assertThat(xp).isEqualTo(120);
        assertThat(user.getCurrentXp()).isEqualTo(120);
        assertThat(users.data.get("u1").getCurrentXp()).isEqualTo(120);
        assertThat(service.getActiveTasks(user.getId()).isEmpty()).isTrue();
    }

    @Test
    void idLookupIgnoresCaseAndSpaces() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-abc12345", "Run", "d", 10), user.getId());

        assertThat(service.completeTask("  t-ABC12345 ", user)).isEqualTo(10);
    }

    @Test
    void completedQuestCannotBeCompletedTwice() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "Run", "d", 50), user.getId());
        service.completeTask("T-1", user);

        assertThatThrownBy(() -> service.completeTask("T-1", user)).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getCurrentXp()).isEqualTo(50);
    }

    @Test
    void globalQuestRequiresScore() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new GlobalQuest("G-1", "Boss", "d", 300, 90), user.getId());

        assertThatThrownBy(() -> service.completeTask("G-1", user)).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getCurrentXp()).isZero();
    }

    @Test
    void globalQuestAwardsBonusXp() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new GlobalQuest("G-1", "Boss", "d", 300, 90), user.getId());

        assertThat(service.completeTask("G-1", user, 95)).isEqualTo(600);
        assertThat(user.getCurrentXp()).isEqualTo(600);
    }

    @Test
    void invalidScoreLeavesQuestActive() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new GlobalQuest("G-1", "Boss", "d", 300, 90), user.getId());

        assertThatThrownBy(() -> service.completeTask("G-1", user, 150)).isInstanceOf(RuntimeException.class);
        assertThat(service.getActiveTasks(user.getId()).size()).isEqualTo(1);
        assertThat(user.getCurrentXp()).isZero();
    }

    @Test
    void failedProfileSaveRollsBackTaskAndKeepsUserUntouched() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "Run", "d", 100), user.getId());
        users.failOnSave = true;

        assertThatThrownBy(() -> service.completeTask("T-1", user)).isInstanceOf(DataAccessException.class);

        assertThat(user.getCurrentXp()).isZero();                                     // память не изменилась
        assertThat(service.getActiveTasks(user.getId()).size()).isEqualTo(1);        // задача снова активна (rollback)
    }

    @Test
    void duplicateTaskIdIsRejectedInsteadOfOverwritten() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "First", "d", 10), user.getId());

        assertThatThrownBy(() -> service.assignTask(new DailyTask("T-1", "Second", "d", 99), user.getId()))
                .isInstanceOf(DataAccessException.class);
        assertThat(tasks.data.get("T-1").getTitle()).isEqualTo("First");
    }

    @Test
    void nonPositiveXpIsRejected() {
        User user = newUser();

        assertThatThrownBy(() -> service().assignTask(new DailyTask("T-1", "Bad", "d", 0), user.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankTitleIsRejected() {
        User user = newUser();

        assertThatThrownBy(() -> service().assignTask(new DailyTask("T-1", "   ", "d", 10), user.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completedTasksAreListedSeparately() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "Run", "d", 10), user.getId());
        service.assignTask(new DailyTask("T-2", "Read", "d", 20), user.getId());
        service.completeTask("T-1", user);

        assertThat(service.getCompletedTasks(user.getId()).size()).isEqualTo(1);
        assertThat(service.getCompletedTasks(user.getId()).get(0).getId()).isEqualTo("T-1");
        assertThat(service.getActiveTasks(user.getId()).size()).isEqualTo(1);
        assertThat(service.getTasks(user.getId()).size()).isEqualTo(2);
    }

    @Test
    void deleteRemovesOwnTask() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "Run", "d", 10), user.getId());

        service.deleteTask(user.getId(), " t-1 ");

        assertThat(tasks.data.containsKey("T-1")).isFalse();
    }

    @Test
    void cannotDeleteSomeoneElsesTask() {
        User owner = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "Run", "d", 10), owner.getId());

        assertThatThrownBy(() -> service.deleteTask("intruder", "T-1")).isInstanceOf(IllegalArgumentException.class);
        assertThat(tasks.data.containsKey("T-1")).isTrue();
    }
}
