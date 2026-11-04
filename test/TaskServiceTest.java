import db.TaskRepository;
import db.TransactionRunner;
import db.UserRepository;
import exception.DataAccessException;
import model.DailyTask;
import model.GlobalQuest;
import model.PerformanceGrade;
import model.Task;
import model.User;
import model.WeeklyTask;
import org.junit.jupiter.api.Test;
import service.CompletionResult;
import service.EvaluationService;
import service.TaskService;
import service.UserService;

import java.sql.Connection;
import java.time.LocalDate;
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
            data.put(u.getId(), new User(u.getId(), u.getName(), u.getLevel(), u.getCurrentXp(), u.getRank(), u.getAvatarPath()));
            return u;
        }
        public Optional<User> findById(String id) { return Optional.ofNullable(data.get(id)); }
        public Optional<User> findByName(String n) { return data.values().stream().filter(u -> u.getName().equalsIgnoreCase(n)).findFirst(); }
        public Map<String, String> hashes = new HashMap<>();
        public Optional<String> findPasswordHash(String userId) { return Optional.ofNullable(hashes.get(userId)); }
        public void setPasswordHash(Connection c, String userId, String hash) {
            if (!data.containsKey(userId)) throw new DataAccessException("User not found: " + userId);
            hashes.put(userId, hash);
        }
        public List<User> findAll() { return new ArrayList<>(data.values()); }
        public boolean deleteById(String id) { return data.remove(id) != null; }
        public List<User> findTopByXp(int limit) {
            return data.values().stream()
                    .sorted(Comparator.comparingInt(User::getCurrentXp).reversed().thenComparing(User::getName))
                    .limit(limit)
                    .toList();
        }
    }

    static class MemTasks implements TaskRepository {
        Map<String, Task> data = new LinkedHashMap<>();
        Map<String, String> owner = new HashMap<>();

        public void insertTask(Task t, String userId) {
            if (data.containsKey(t.getId())) throw new DataAccessException("Task with ID already exists: " + t.getId());
            data.put(t.getId(), copy(t)); owner.put(t.getId(), userId);
        }
        public void insertTask(Connection c, Task t, String userId) { insertTask(t, userId); }
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
            r.setDeadline(t.getDeadline());
            r.setGrade(t.getGrade());
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

        int xp = service.completeTask("T-1", user, 50);

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

        assertThat(service.completeTask("  t-ABC12345 ", user, 50)).isEqualTo(10);
    }

    @Test
    void completedQuestCannotBeCompletedTwice() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "Run", "d", 50), user.getId());
        service.completeTask("T-1", user, 50);

        assertThatThrownBy(() -> service.completeTask("T-1", user, 50)).isInstanceOf(IllegalArgumentException.class);
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

        assertThatThrownBy(() -> service.completeTask("T-1", user, 50)).isInstanceOf(DataAccessException.class);

        assertThat(user.getCurrentXp()).isZero();
        assertThat(service.getActiveTasks(user.getId()).size()).isEqualTo(1);
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
        service.completeTask("T-1", user, 50);

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

    @Test
    void dailyQuestRequiresScore() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "Run", "d", 100), user.getId());

        assertThatThrownBy(() -> service.completeTask("T-1", user)).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getCurrentXp()).isZero();
    }

    @Test
    void dailyQuestIsGradedAndGradeIsPersisted() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new DailyTask("T-1", "Run", "d", 100), user.getId());

        CompletionResult result = service.complete("T-1", user, 95);

        assertThat(result.grade()).isEqualTo(PerformanceGrade.S);
        assertThat(result.xp()).isEqualTo(200);
        assertThat(user.getCurrentXp()).isEqualTo(200);
        assertThat(tasks.data.get("T-1").getGrade()).isEqualTo(PerformanceGrade.S);
    }

    @Test
    void weeklyQuestBonusDependsOnGrade() {
        User user = newUser();
        TaskService service = service();
        service.assignTask(new WeeklyTask("W-1", "Read", "d", 150), user.getId());

        CompletionResult result = service.complete("W-1", user, 80);

        assertThat(result.grade()).isEqualTo(PerformanceGrade.B);
        assertThat(result.xp()).isEqualTo(210);
    }

    @Test
    void activeTasksAreSortedByDeadlineWithUndatedLast() {
        User user = newUser();
        TaskService service = service();
        LocalDate today = LocalDate.now();
        DailyTask later = new DailyTask("T-late", "Later", "d", 10);
        later.setDeadline(today.plusDays(5));
        DailyTask soon = new DailyTask("T-soon", "Soon", "d", 10);
        soon.setDeadline(today.plusDays(1));
        DailyTask none = new DailyTask("T-none", "None", "d", 500);
        service.assignTask(later, user.getId());
        service.assignTask(none, user.getId());
        service.assignTask(soon, user.getId());

        List<String> order = service.getActiveTasks(user.getId()).stream().map(Task::getId).toList();

        assertThat(order).isEqualTo(List.of("T-soon", "T-late", "T-none"));
    }

    @Test
    void deadlineSurvivesRoundTrip() {
        User user = newUser();
        TaskService service = service();
        DailyTask task = new DailyTask("T-1", "Run", "d", 10);
        task.setDeadline(LocalDate.of(2030, 1, 15));
        service.assignTask(task, user.getId());

        assertThat(service.getTasks(user.getId()).get(0).getDeadline()).isEqualTo(LocalDate.of(2030, 1, 15));
    }

    @Test
    void overdueIsDetectedOnlyForActiveTasksWithPastDeadline() {
        LocalDate today = LocalDate.of(2026, 11, 3);
        DailyTask overdue = new DailyTask("a", "a", "d", 10);
        overdue.setDeadline(today.minusDays(1));
        DailyTask dueToday = new DailyTask("b", "b", "d", 10);
        dueToday.setDeadline(today);
        DailyTask done = new DailyTask("c", "c", "d", 10);
        done.setDeadline(today.minusDays(3));
        done.setCompleted(true);

        assertThat(overdue.isOverdue(today)).isTrue();
        assertThat(dueToday.isOverdue(today)).isFalse();
        assertThat(done.isOverdue(today)).isFalse();
        assertThat(new DailyTask("d", "d", "d", 10).isOverdue(today)).isFalse();
    }
}
