package model;

import java.io.Serializable;

public abstract class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String title;
    private final String description;
    private final int baseXp;
    private boolean completed;

    public Task(String id, String title, String description, int baseXp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.baseXp = baseXp;
        this.completed = false;
    }

    public void complete() {
        this.completed = true;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getBaseXp() { return baseXp; }
    public boolean isCompleted() { return completed; }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (XP: %d, Completed: %b)", id, title, baseXp, completed);
    }
}