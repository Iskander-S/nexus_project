package model;

public class GlobalQuest extends Task implements Evaluatable {
    private static final long serialVersionUID = 1L;

    private final int targetScore;

    public GlobalQuest(String id, String title, String description, int targetScore) {
        this(id, title, description, 300, targetScore);
    }

    public GlobalQuest(String id, String title, String description, int baseXp, int targetScore) {
        super(id, title, description, baseXp);
        if (targetScore < 1 || targetScore > 100) {
            // оценка валидируется в диапазоне 0..100, цель вне диапазона была бы недостижима или бессмысленна
            throw new IllegalArgumentException("Target score must be between 1 and 100: " + targetScore);
        }
        this.targetScore = targetScore;
    }

    /**
     * Достигнута цель квеста -> S. Раньше было {@code actualScore >= 90 || actualScore >= targetScore},
     * из-за чего при цели 100 результат 90 давал максимальную оценку.
     */
    @Override
    public PerformanceGrade evaluatePerformance(int actualScore) {
        if (actualScore >= targetScore) {
            return PerformanceGrade.S;
        } else if (actualScore >= 85) {
            return PerformanceGrade.A;
        } else if (actualScore >= 80) {
            return PerformanceGrade.B;
        } else if (actualScore >= 70) {
            return PerformanceGrade.C;
        } else if (actualScore >= 60) {
            return PerformanceGrade.D;
        } else {
            return PerformanceGrade.E;
        }
    }

    @Override
    public int calculateBonusXp(int baseXp, PerformanceGrade grade) {
        return (int) (baseXp * grade.getBonusMultiplier());
    }

    public int getTargetScore() {
        return targetScore;
    }
}
