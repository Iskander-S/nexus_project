package model;

public enum Rank {
    S_RANK("S-Rank", 2000),
    A_RANK("A-Rank", 1600),
    B_RANK("B-Rank", 1200),
    C_RANK("C-Rank", 800),
    D_RANK("D-Rank", 400),
    E_RANK("E-Rank", 0);

    private final String displayName;
    private final int minXp;

    Rank(String displayName, int minXp) {
        this.displayName = displayName;
        this.minXp = minXp;
    }

    public String getDisplayName() { return displayName; }
    public int getMinXp() { return minXp; }

    public static Rank getRankByXp(int xp) {
        for (Rank rank : values()) {
            if (xp >= rank.minXp) {
                return rank;
            }
        }
        return E_RANK;
    }
}