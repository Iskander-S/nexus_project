package service;

import model.Achievement;
import model.PerformanceGrade;

import java.util.List;

public record CompletionResult(int xp, PerformanceGrade grade, List<Achievement> unlocked) {

    public CompletionResult {
        unlocked = List.copyOf(unlocked);
    }

    public CompletionResult withUnlocked(List<Achievement> achievements) {
        return new CompletionResult(xp, grade, achievements);
    }
}
