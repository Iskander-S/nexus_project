package service;

import model.Achievement;
import model.Task;

import java.util.List;

public interface AchievementRule {
    Achievement achievement();

    boolean isEarned(List<Task> completedTasks);
}
