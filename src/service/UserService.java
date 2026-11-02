package service;

import db.UserRepository;
import exception.DataAccessException;
import model.User;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getOrCreateUser(String userId, String defaultName) {
        return userRepository.findById(userId).orElseGet(() -> {
            User newUser = new User(userId, defaultName);
            userRepository.save(newUser);
            return newUser;
        });
    }

    public void updateProfile(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or User ID cannot be null");
        }
        userRepository.save(user);
    }
}