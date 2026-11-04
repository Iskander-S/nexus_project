package service;

import model.PerformanceGrade;

public record EvaluationResult(PerformanceGrade grade, int baseXp, int bonusXp) {

    public int totalXp() {
        return baseXp + bonusXp;
    }
}
