package service;

import db.SqlErrors;
import db.UserRepository;
import exception.AuthException;
import exception.AuthException.Reason;
import exception.DataAccessException;
import model.User;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class UserService {
    public static final int LEADERBOARD_SIZE = 10;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findUser(String userId) {
        return userRepository.findById(userId);
    }

    public User getOrCreateUser(String userId, String defaultName) {
        return userRepository.findById(userId).orElseGet(() -> {
            User newUser = new User(userId, defaultName);
            userRepository.save(newUser);
            return newUser;
        });
    }

    public void updateProfile(User user) {
        requireValid(user);
        userRepository.save(user);
    }

    public void updateProfile(Connection conn, User user) {
        requireValid(user);
        userRepository.save(conn, user);
    }

    public List<User> getLeaderboard() {
        return getLeaderboard(LEADERBOARD_SIZE);
    }

    public List<User> getLeaderboard(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Leaderboard size must be positive: " + limit);
        }
        return userRepository.findTopByXp(limit);
    }

    public void renameUser(User user, String newName) {
        requireValid(user);
        String name = UsernamePolicy.requireValid(newName);
        if (name.equals(user.getName())) {
            return;
        }
        Optional<User> owner = userRepository.findByName(name);
        if (owner.isPresent() && !owner.get().getId().equals(user.getId())) {
            throw nameTaken(name);
        }

        User updated = new User(user.getId(), name, user.getLevel(), user.getCurrentXp(), user.getRank(),
                user.getAvatarPath());
        try {
            userRepository.save(updated);
        } catch (DataAccessException e) {
            if (SqlErrors.isUniqueViolation(e)) {
                throw nameTaken(name);
            }
            throw e;
        }
        user.rename(name);
    }

    private static AuthException nameTaken(String name) {
        return new AuthException(Reason.USER_EXISTS, "User already exists: " + name);
    }

    private static void requireValid(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or User ID cannot be null");
        }
    }
}
