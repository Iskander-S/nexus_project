package service;

import db.UserRepository;
import model.User;

import java.sql.Connection;
import java.util.Optional;

public class UserService {
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

    /** Сохраняет профиль в рамках уже открытой транзакции. */
    public void updateProfile(Connection conn, User user) {
        requireValid(user);
        userRepository.save(conn, user);
    }

    private static void requireValid(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or User ID cannot be null");
        }
    }
}