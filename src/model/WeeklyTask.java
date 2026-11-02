package model;

public class WeeklyTask extends Task {
    public WeeklyTask(String id, String title, String description) {
        super(id, title, description, 150);
    }

    public WeeklyTask(String id, String title, String description, int baseXp) {
        super(id, title, description, baseXp);
    }
}