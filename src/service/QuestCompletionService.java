package service;

import model.User;

public class QuestCompletionService {
    private final TaskService taskService;
    private final AchievementService achievementService;

    public QuestCompletionService(TaskService taskService, AchievementService achievementService) {
        this.taskService = taskService;
        this.achievementService = achievementService;
    }

    public CompletionResult complete(String taskId, User user, Integer score) {
        CompletionResult result = taskService.complete(taskId, user, score);
        return result.withUnlocked(achievementService.checkAndUnlock(user.getId()));
    }
}
