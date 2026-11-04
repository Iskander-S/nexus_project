import db.AchievementRepository;
import db.TransactionRunner;
import model.Achievement;
import model.DailyTask;
import model.PerformanceGrade;
import model.User;
import org.junit.jupiter.api.Test;
import service.AchievementService;
import service.EvaluationService;
import service.FirstBloodRule;
import service.OverachieverRule;
import service.QuestCompletionService;
import service.TaskService;
import service.UserService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementServiceTest {

    static class MemAchievements implements AchievementRepository {
        Map<String, Achievement> catalog = new LinkedHashMap<>();
        Map<String, List<String>> unlocked = new LinkedHashMap<>();

        public void upsertAll(List<Achievement> achievements) {
            achievements.forEach(a -> catalog.put(a.id(), a));
        }

        public List<Achievement> findAll() {
            return new ArrayList<>(catalog.values());
        }

        public List<Achievement> findUnlockedByUser(String userId) {
            return unlocked.getOrDefault(userId, List.of()).stream().map(catalog::get).toList();
        }

        public boolean unlock(String userId, String achievementId) {
            List<String> ids = unlocked.computeIfAbsent(userId, k -> new ArrayList<>());
            if (ids.contains(achievementId)) {
                return false;
            }
            ids.add(achievementId);
            return true;
        }
    }

    TaskServiceTest.MemUsers users = new TaskServiceTest.MemUsers();
    TaskServiceTest.MemTasks tasks = new TaskServiceTest.MemTasks();
    MemAchievements achievements = new MemAchievements();
    TransactionRunner tx = work -> {
        try {
            work.execute(null);
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    };

    AchievementService achievementService() {
        AchievementService service = new AchievementService(achievements, tasks, AchievementService.defaultRules());
        service.syncCatalog();
        return service;
    }

    TaskService taskService() {
        return new TaskService(tasks, new UserService(users), new EvaluationService(), tx);
    }

    User newUser() {
        User user = new User("u1", "Tester");
        users.save(user);
        return user;
    }

    @Test
    void catalogIsSynchronizedFromRules() {
        achievementService();

        assertThat(achievements.catalog.containsKey(FirstBloodRule.ID)).isTrue();
        assertThat(achievements.catalog.containsKey(OverachieverRule.ID)).isTrue();
    }

    @Test
    void noAchievementsWithoutCompletedQuests() {
        User user = newUser();
        taskService().assignTask(new DailyTask("T-1", "Run", "d", 10), user.getId());

        assertThat(achievementService().checkAndUnlock(user.getId()).isEmpty()).isTrue();
    }

    @Test
    void firstCompletedQuestUnlocksFirstBloodOnce() {
        User user = newUser();
        TaskService taskService = taskService();
        AchievementService service = achievementService();
        taskService.assignTask(new DailyTask("T-1", "Run", "d", 10), user.getId());
        taskService.assignTask(new DailyTask("T-2", "Read", "d", 10), user.getId());

        taskService.completeTask("T-1", user, 10);
        List<Achievement> first = service.checkAndUnlock(user.getId());
        taskService.completeTask("T-2", user, 10);
        List<Achievement> second = service.checkAndUnlock(user.getId());

        assertThat(first.size()).isEqualTo(1);
        assertThat(first.get(0).id()).isEqualTo(FirstBloodRule.ID);
        assertThat(second.isEmpty()).isTrue();
        assertThat(service.getUnlocked(user.getId()).size()).isEqualTo(1);
    }

    @Test
    void fiveSGradesUnlockOverachiever() {
        User user = newUser();
        TaskService taskService = taskService();
        AchievementService service = achievementService();
        QuestCompletionService flow = new QuestCompletionService(taskService, service);

        List<Achievement> lastUnlocked = List.of();
        for (int i = 1; i <= 5; i++) {
            taskService.assignTask(new DailyTask("T-" + i, "Quest " + i, "d", 10), user.getId());
            lastUnlocked = flow.complete("T-" + i, user, 100).unlocked();
            if (i < 5) {
                assertThat(tasks.data.get("T-" + i).getGrade()).isEqualTo(PerformanceGrade.S);
            }
        }

        assertThat(lastUnlocked.size()).isEqualTo(1);
        assertThat(lastUnlocked.get(0).id()).isEqualTo(OverachieverRule.ID);
        assertThat(service.getUnlocked(user.getId()).size()).isEqualTo(2);
    }

    @Test
    void fourSGradesAreNotEnough() {
        User user = newUser();
        TaskService taskService = taskService();
        QuestCompletionService flow = new QuestCompletionService(taskService, achievementService());

        for (int i = 1; i <= 4; i++) {
            taskService.assignTask(new DailyTask("T-" + i, "Quest " + i, "d", 10), user.getId());
            flow.complete("T-" + i, user, 100);
        }

        assertThat(achievements.unlocked.get(user.getId()).contains(OverachieverRule.ID)).isFalse();
    }

    @Test
    void lowGradesDoNotCountTowardsOverachiever() {
        User user = newUser();
        TaskService taskService = taskService();
        QuestCompletionService flow = new QuestCompletionService(taskService, achievementService());

        for (int i = 1; i <= 5; i++) {
            taskService.assignTask(new DailyTask("T-" + i, "Quest " + i, "d", 10), user.getId());
            flow.complete("T-" + i, user, 50);
        }

        assertThat(achievements.unlocked.get(user.getId()).contains(OverachieverRule.ID)).isFalse();
    }
}
