package service;

import model.Achievement;
import model.PerformanceGrade;
import model.Task;

import java.util.List;

public final class OverachieverRule implements AchievementRule {
    public static final String ID = "overachiever";
    public static final int REQUIRED_S_GRADES = 5;

    private static final Achievement ACHIEVEMENT = new Achievement(
            ID, "Overachiever", "Earn the S grade on " + REQUIRED_S_GRADES + " quests.", "\u2605");

    @Override
    public Achievement achievement() {
        return ACHIEVEMENT;
    }

    @Override
    public boolean isEarned(List<Task> completedTasks) {
        return completedTasks.stream().filter(t -> t.getGrade() == PerformanceGrade.S).count() >= REQUIRED_S_GRADES;
    }
}
