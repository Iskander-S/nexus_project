package db;

import exception.DataAccessException;
import model.DailyTask;
import model.GlobalQuest;
import model.PerformanceGrade;
import model.Task;
import model.WeeklyTask;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2TaskRepositoryImpl implements TaskRepository {

    @Override
    public void insertTask(Task task, String userId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            insertTask(conn, task, userId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save task: " + task.getId(), e);
        }
    }

    @Override
    public void insertTask(Connection conn, Task task, String userId) {
        String sql = "INSERT INTO tasks (id, user_id, task_type, title, description, base_xp, completed, target_score, deadline, grade) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, task.getId());
            pstmt.setString(2, userId);
            pstmt.setString(3, typeOf(task));
            pstmt.setString(4, task.getTitle());
            pstmt.setString(5, task.getDescription());
            pstmt.setInt(6, task.getBaseXp());
            pstmt.setBoolean(7, task.isCompleted());
            setTargetScore(pstmt, 8, task);
            setDeadline(pstmt, 9, task);
            setGrade(pstmt, 10, task);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            if (SqlErrors.isUniqueViolation(e)) {
                throw new DataAccessException("Task with ID already exists: " + task.getId(), e);
            }
            throw new DataAccessException("Failed to save task: " + task.getId(), e);
        }
    }

    @Override
    public void updateTask(Connection conn, Task task, String userId) {
        String sql = "UPDATE tasks SET title = ?, description = ?, base_xp = ?, completed = ?, target_score = ?, " +
                "deadline = ?, grade = ? WHERE id = ? AND user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setInt(3, task.getBaseXp());
            pstmt.setBoolean(4, task.isCompleted());
            setTargetScore(pstmt, 5, task);
            setDeadline(pstmt, 6, task);
            setGrade(pstmt, 7, task);
            pstmt.setString(8, task.getId());
            pstmt.setString(9, userId);

            if (pstmt.executeUpdate() == 0) {
                throw new DataAccessException("Task not found for this user: " + task.getId());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update task: " + task.getId(), e);
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
        String sql = "SELECT * FROM tasks WHERE user_id = ? ORDER BY deadline ASC NULLS LAST, base_xp DESC";

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
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete task with ID: " + id, e);
        }
    }

    private static String typeOf(Task task) {
        if (task instanceof DailyTask) return "DAILY";
        if (task instanceof WeeklyTask) return "WEEKLY";
        if (task instanceof GlobalQuest) return "GLOBAL";
        throw new IllegalArgumentException("Unsupported task type: " + task.getClass().getName());
    }

    private static void setTargetScore(PreparedStatement pstmt, int index, Task task) throws SQLException {
        if (task instanceof GlobalQuest gq) {
            pstmt.setInt(index, gq.getTargetScore());
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }

    private static void setDeadline(PreparedStatement pstmt, int index, Task task) throws SQLException {
        if (task.getDeadline() != null) {
            pstmt.setObject(index, task.getDeadline());
        } else {
            pstmt.setNull(index, Types.DATE);
        }
    }

    private static void setGrade(PreparedStatement pstmt, int index, Task task) throws SQLException {
        if (task.getGrade() != null) {
            pstmt.setString(index, task.getGrade().name());
        } else {
            pstmt.setNull(index, Types.VARCHAR);
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
        task.setDeadline(rs.getObject("deadline", LocalDate.class));
        String grade = rs.getString("grade");
        if (grade != null) {
            task.setGrade(PerformanceGrade.valueOf(grade));
        }
        return task;
    }
}
