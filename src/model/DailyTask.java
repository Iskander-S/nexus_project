package model;

public class DailyTask extends Task {
    public DailyTask(String id, String title, String description) {
        super(id, title, description, 50);
    }

    public DailyTask(String id, String title, String description, int baseXp) {
        super(id, title, description, baseXp);
    }
}