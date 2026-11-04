package service;

import db.AchievementRepository;
import db.TaskRepository;
import model.Achievement;
import model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AchievementService {
    private final AchievementRepository achievementRepository;
    private final TaskRepository taskRepository;
    private final List<AchievementRule> rules;

    public AchievementService(AchievementRepository achievementRepository,
                              TaskRepository taskRepository,
                              List<AchievementRule> rules) {
        this.achievementRepository = achievementRepository;
        this.taskRepository = taskRepository;
        this.rules = List.copyOf(rules);
    }

    public static List<AchievementRule> defaultRules() {
        return List.of(new FirstBloodRule(), new OverachieverRule());
    }

    public void syncCatalog() {
        achievementRepository.upsertAll(rules.stream().map(AchievementRule::achievement).toList());
    }

    public List<Achievement> checkAndUnlock(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID must not be empty");
        }
        List<Task> completed = taskRepository.findByUserId(userId).stream()
                .filter(Task::isCompleted)
                .toList();
        Set<String> alreadyUnlocked = achievementRepository.findUnlockedByUser(userId).stream()
                .map(Achievement::id)
                .collect(Collectors.toSet());

        List<Achievement> newlyUnlocked = new ArrayList<>();
        for (AchievementRule rule : rules) {
            Achievement achievement = rule.achievement();
            if (alreadyUnlocked.contains(achievement.id())) {
                continue;
            }
            if (rule.isEarned(completed) && achievementRepository.unlock(userId, achievement.id())) {
                newlyUnlocked.add(achievement);
            }
        }
        return newlyUnlocked;
    }

    public List<Achievement> getUnlocked(String userId) {
        return achievementRepository.findUnlockedByUser(userId);
    }

    public List<Achievement> getCatalog() {
        return achievementRepository.findAll();
    }
}
