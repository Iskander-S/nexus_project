package service;

import model.User;
import java.io.*;
import java.util.Optional;

public class FileHandler {

    public void saveUserData(User user, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(user);
            System.out.printf("[PERSISTENCE] Progress saved successfully to '%s'.%n", filePath);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save user progress: " + e.getMessage());
        }
    }

    public Optional<User> loadUserData(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return Optional.of((User) ois.readObject());
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.err.println("[PERSISTENCE] Could not load saved file: " + e.getMessage());
            return Optional.empty();
        }
    }
}