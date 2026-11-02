package ui;

import model.GlobalQuest;
import model.Task;
import model.WeeklyTask;

public enum QuestKind {
    DAILY("Daily", "daily", 50, "A small goal for today."),
    WEEKLY("Weekly", "weekly", 150, "A bigger goal for the week."),
    GLOBAL("Global", "global", 300, "A boss quest: you enter a score from 0 to 100, and a great result earns bonus XP.");

    private final String label;
    private final String css;
    private final int defaultXp;
    private final String hint;

    QuestKind(String label, String css, int defaultXp, String hint) {
        this.label = label;
        this.css = css;
        this.defaultXp = defaultXp;
        this.hint = hint;
    }

    public String label() { return label; }
    public String css() { return css; }
    public int defaultXp() { return defaultXp; }
    public String hint() { return hint; }

    public static QuestKind of(Task task) {
        if (task instanceof GlobalQuest) return GLOBAL;
        if (task instanceof WeeklyTask) return WEEKLY;
        return DAILY;
    }
}
