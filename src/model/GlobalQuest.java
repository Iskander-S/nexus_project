package model;

public class GlobalQuest extends Task implements Evaluatable {
    private final int targetScore;

    public GlobalQuest(String id, String title, String description, int targetScore) {
        super(id, title, description, 300);
        this.targetScore = targetScore;
    }

    public GlobalQuest(String id, String title, String description, int baseXp, int targetScore) {
        super(id, title, description, baseXp);
        this.targetScore = targetScore;
    }

    @Override
    public PerformanceGrade evaluatePerformance(int actualScore) {
        if (actualScore >= 90 || actualScore >= targetScore) {
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