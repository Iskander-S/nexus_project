package model;

public interface Evaluatable {
    PerformanceGrade evaluatePerformance(int actualScore);
    int calculateBonusXp(int baseXp, PerformanceGrade grade);
}