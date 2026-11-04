package db;

import exception.DataAccessException;
import model.Rank;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2UserRepositoryImpl implements UserRepository {

    @Override
    public User save(User user) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return save(conn, user);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user with ID: " + user.getId(), e);
        }
    }

    @Override
    public User save(Connection conn, User user) {
        String sql = "MERGE INTO users (id, name, level, current_xp, rank, avatar_path) KEY(id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getName());
            pstmt.setInt(3, user.getLevel());
            pstmt.setInt(4, user.getCurrentXp());
            pstmt.setString(5, user.getRank().name());
            if (user.getAvatarPath() != null) {
                pstmt.setString(6, user.getAvatarPath());
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }

            pstmt.executeUpdate();
            return user;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user with ID: " + user.getId(), e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        return findOne("SELECT * FROM users WHERE id = ?", id,
                "Failed to load user with ID: " + id);
    }

    @Override
    public Optional<User> findByName(String name) {
        return findOne("SELECT * FROM users WHERE LOWER(name) = LOWER(?) LIMIT 1", name,
                "Failed to load user with name: " + name);
    }

    @Override
    public Optional<String> findPasswordHash(String userId) {
        String sql = "SELECT password_hash FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load credentials for user ID: " + userId, e);
        }
        return Optional.empty();
    }

    @Override
    public void setPasswordHash(Connection conn, String userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, passwordHash);
            pstmt.setString(2, userId);
            if (pstmt.executeUpdate() == 0) {
                throw new DataAccessException("User not found: " + userId);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save credentials for user ID: " + userId, e);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load all users", e);
        }
        return users;
    }

    @Override
    public List<User> findTopByXp(int limit) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY current_xp DESC, name ASC LIMIT ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load leaderboard", e);
        }
        return users;
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete user with ID: " + id, e);
        }
    }

    private Optional<User> findOne(String sql, String param, String errorMessage) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, param);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
        return Optional.empty();
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("id"),
                rs.getString("name"),
                rs.getInt("level"),
                rs.getInt("current_xp"),
                Rank.valueOf(rs.getString("rank")),
                rs.getString("avatar_path")
        );
    }
}
