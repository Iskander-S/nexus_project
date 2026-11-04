package service;

import model.Achievement;
import model.Task;

import java.util.List;

public final class FirstBloodRule implements AchievementRule {
    public static final String ID = "first_blood";

    private static final Achievement ACHIEVEMENT = new Achievement(
            ID, "First Blood", "Complete your very first quest.", "\u2694");

    @Override
    public Achievement achievement() {
        return ACHIEVEMENT;
    }

    @Override
    public boolean isEarned(List<Task> completedTasks) {
        return !completedTasks.isEmpty();
    }
}
