package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private final String name;
    private int level = 1;
    private int currentXp = 0;
    private Rank rank = Rank.E_RANK;
    private final List<Task> tasks = new ArrayList<>();

    public User(String name) {
        this.id = "user_" + java.util.UUID.randomUUID();
        this.name = name;
    }

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public User(String id, String name, int level, int currentXp, Rank rank) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.currentXp = currentXp;
        this.rank = rank;
    }

    public void addXp(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("XP amount must not be negative: " + amount);
        }
        this.currentXp += amount;
        this.level = 1 + (this.currentXp / 200);
        this.rank = Rank.getRankByXp(this.currentXp);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getCurrentXp() { return currentXp; }
    public Rank getRank() { return rank; }
    public List<Task> getTasks() { return tasks; }
}