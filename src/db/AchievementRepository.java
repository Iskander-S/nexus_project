package db;

import model.Achievement;

import java.util.List;

public interface AchievementRepository {
    void upsertAll(List<Achievement> achievements);

    List<Achievement> findAll();

    List<Achievement> findUnlockedByUser(String userId);

    boolean unlock(String userId, String achievementId);
}
