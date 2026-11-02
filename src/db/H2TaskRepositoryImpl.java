package db;

import exception.DataAccessException;
import model.DailyTask;
import model.GlobalQuest;
import model.Task;
import model.WeeklyTask;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2TaskRepositoryImpl implements TaskRepository {

    @Override
    public Task save(Task task) {
        throw new UnsupportedOperationException("Use saveTaskWithUserId(task, userId) or saveAll(tasks, userId)");
    }

    @Override
    public void saveTaskWithUserId(Task task, String userId) {
        String sql = "MERGE INTO tasks (id, user_id, task_type, title, description, base_xp, completed, target_score) " +
                "KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        String type = switch (task) {
            case DailyTask d -> "DAILY";
            case WeeklyTask w -> "WEEKLY";
            case GlobalQuest g -> "GLOBAL";
            default -> throw new IllegalArgumentException("Unsupported task type: " + task.getClass().getName());
        };

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, task.getId());
            pstmt.setString(2, userId);
            pstmt.setString(3, type);
            pstmt.setString(4, task.getTitle());
            pstmt.setString(5, task.getDescription());
            pstmt.setInt(6, task.getBaseXp());
            pstmt.setBoolean(7, task.isCompleted());

            if (task instanceof GlobalQuest gq) {
                pstmt.setInt(8, gq.getTargetScore());
            } else {
                pstmt.setNull(8, Types.INTEGER);
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save task: " + task.getId(), e);
        }
    }

    @Override
    public void saveAll(List<Task> tasks, String userId) {
        for (Task task : tasks) {
            saveTaskWithUserId(task, userId);
        }
    }

    @Override
    public Optional<Task> findById(String id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find task with ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Task> findByUserId(String userId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE user_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load tasks for user ID: " + userId, e);
        }
        return tasks;
    }

    @Override
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tasks.add(mapRowToTask(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load all tasks", e);
        }
        return tasks;
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete task with ID: " + id, e);
        }
    }

    private Task mapRowToTask(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String title = rs.getString("title");
        String desc = rs.getString("description");
        int baseXp = rs.getInt("base_xp");
        boolean completed = rs.getBoolean("completed");

        Task task = switch (rs.getString("task_type")) {
            case "DAILY" -> new DailyTask(id, title, desc, baseXp);
            case "WEEKLY" -> new WeeklyTask(id, title, desc, baseXp);
            case "GLOBAL" -> new GlobalQuest(id, title, desc, baseXp, rs.getInt("target_score"));
            default -> throw new IllegalStateException("Unknown task type in database for task " + id);
        };

        task.setCompleted(completed);
        return task;
    }
}