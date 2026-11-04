package model;

public class GlobalQuest extends Task {
    private static final long serialVersionUID = 1L;

    private final int targetScore;

    public GlobalQuest(String id, String title, String description, int targetScore) {
        this(id, title, description, 300, targetScore);
    }

    public GlobalQuest(String id, String title, String description, int baseXp, int targetScore) {
        super(id, title, description, baseXp);
        if (targetScore < 1 || targetScore > 100) {
            throw new IllegalArgumentException("Target score must be between 1 and 100: " + targetScore);
        }
        this.targetScore = targetScore;
    }

    @Override
    public int getTargetScore() {
        return targetScore;
    }
}
