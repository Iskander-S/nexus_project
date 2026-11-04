package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;

public abstract class Task implements Serializable, Evaluatable {
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_TARGET_SCORE = 90;

    public static final Comparator<Task> DEADLINE_ORDER = Comparator
            .comparing(Task::getDeadline, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
            .thenComparing(Comparator.comparingInt((Task t) -> t.getBaseXp()).reversed());

    private final String id;
    private final String title;
    private final String description;
    private final int baseXp;
    private boolean completed;
    private LocalDate deadline;
    private PerformanceGrade grade;

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

    public boolean isOverdue(LocalDate today) {
        return !completed && deadline != null && deadline.isBefore(today);
    }

    public int getTargetScore() {
        return DEFAULT_TARGET_SCORE;
    }

    @Override
    public PerformanceGrade evaluatePerformance(int actualScore) {
        if (actualScore >= getTargetScore()) {
            return PerformanceGrade.S;
        } else if (actualScore >= 85) {
            return PerformanceGrade.A;
        } else if (actualScore >= 80) {
            return PerformanceGrade.B;
        } else if (actualScore >= 70) {
            return PerformanceGrade.C;
        } else if (actualScore >= 60) {
            return PerformanceGrade.D;
        }
        return PerformanceGrade.E;
    }

    @Override
    public int calculateBonusXp(int baseXp, PerformanceGrade grade) {
        return (int) (baseXp * grade.getBonusMultiplier());
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getBaseXp() { return baseXp; }
    public boolean isCompleted() { return completed; }
    public LocalDate getDeadline() { return deadline; }
    public PerformanceGrade getGrade() { return grade; }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public void setGrade(PerformanceGrade grade) {
        this.grade = grade;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (XP: %d, Completed: %b)", id, title, baseXp, completed);
    }
}
