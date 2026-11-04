package db;

import exception.DataAccessException;
import model.Achievement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class H2AchievementRepositoryImpl implements AchievementRepository {

    @Override
    public void upsertAll(List<Achievement> achievements) {
        String sql = "MERGE INTO achievements (id, title, description, icon) KEY(id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Achievement a : achievements) {
                pstmt.setString(1, a.id());
                pstmt.setString(2, a.title());
                pstmt.setString(3, a.description());
                pstmt.setString(4, a.icon());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save achievements catalog", e);
        }
    }

    @Override
    public List<Achievement> findAll() {
        List<Achievement> result = new ArrayList<>();
        String sql = "SELECT id, title, description, icon FROM achievements ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load achievements", e);
        }
        return result;
    }

    @Override
    public List<Achievement> findUnlockedByUser(String userId) {
        List<Achievement> result = new ArrayList<>();
        String sql = "SELECT a.id, a.title, a.description, a.icon FROM achievements a " +
                "JOIN user_achievements ua ON ua.achievement_id = a.id " +
                "WHERE ua.user_id = ? ORDER BY ua.unlocked_at, a.id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load achievements for user ID: " + userId, e);
        }
        return result;
    }

    @Override
    public boolean unlock(String userId, String achievementId) {
        String sql = "INSERT INTO user_achievements (user_id, achievement_id) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, achievementId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (SqlErrors.isUniqueViolation(e)) {
                return false;
            }
            throw new DataAccessException("Failed to unlock achievement " + achievementId, e);
        }
    }

    private static Achievement map(ResultSet rs) throws SQLException {
        return new Achievement(rs.getString("id"), rs.getString("title"),
                rs.getString("description"), rs.getString("icon"));
    }
}
