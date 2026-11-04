package service;

import db.UserRepository;
import model.User;

import java.nio.file.Path;

public class AvatarService {
    private final UserRepository userRepository;
    private final AvatarStorage storage;

    public AvatarService(UserRepository userRepository, AvatarStorage storage) {
        this.userRepository = userRepository;
        this.storage = storage;
    }

    public void changeAvatar(User user, Path source) {
        requireValid(user);
        String previous = user.getAvatarPath();
        String stored = storage.store(user.getId(), source);
        try {
            userRepository.save(copyWithAvatar(user, stored));
        } catch (RuntimeException e) {
            storage.delete(stored);
            throw e;
        }
        user.changeAvatarPath(stored);
        storage.delete(previous);
    }

    public void removeAvatar(User user) {
        requireValid(user);
        String previous = user.getAvatarPath();
        if (previous == null) {
            return;
        }
        userRepository.save(copyWithAvatar(user, null));
        user.changeAvatarPath(null);
        storage.delete(previous);
    }

    private static User copyWithAvatar(User user, String avatarPath) {
        return new User(user.getId(), user.getName(), user.getLevel(), user.getCurrentXp(), user.getRank(), avatarPath);
    }

    private static void requireValid(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or User ID cannot be null");
        }
    }
}
