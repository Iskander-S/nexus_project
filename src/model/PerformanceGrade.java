package model;

public enum PerformanceGrade {
    E(0.0),
    D(0.1),
    C(0.2),
    B(0.4),
    A(0.6),
    S(1.0);

    private final double bonusMultiplier;

    PerformanceGrade(double bonusMultiplier) {
        this.bonusMultiplier = bonusMultiplier;
    }

    public double getBonusMultiplier() {
        return bonusMultiplier;
    }
}