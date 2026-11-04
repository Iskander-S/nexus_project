package service;

import db.SqlErrors;
import db.TransactionRunner;
import db.UserRepository;
import exception.AuthException;
import exception.AuthException.Reason;
import exception.DataAccessException;
import model.User;

public class AuthService {
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 128;

    private final UserRepository userRepository;
    private final TaskService taskService;
    private final TransactionRunner transactions;

    public AuthService(UserRepository userRepository, TaskService taskService, TransactionRunner transactions) {
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.transactions = transactions;
    }

    public User register(String username, String password) {
        String name = UsernamePolicy.requireValid(username);
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new AuthException(Reason.WEAK_PASSWORD,
                    "Password must be " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + " characters");
        }
        if (userRepository.findByName(name).isPresent()) {
            throw userExists(name);
        }

        User user = new User(name);
        String hash = PasswordHasher.hash(password);

        transactions.inTransaction(conn -> {
            try {
                userRepository.save(conn, user);
            } catch (DataAccessException e) {
                if (SqlErrors.isUniqueViolation(e)) {
                    throw userExists(name);
                }
                throw e;
            }
            userRepository.setPasswordHash(conn, user.getId(), hash);
            taskService.assignStarterQuests(conn, user.getId());
        });
        return user;
    }

    public User login(String username, String password) {
        String name = username == null ? "" : username.trim();
        User user = userRepository.findByName(name)
                .orElseThrow(() -> new AuthException(Reason.USER_NOT_FOUND, "User not found: " + name));

        String stored = userRepository.findPasswordHash(user.getId()).orElse(null);
        if (stored == null) {
            throw new AuthException(Reason.NO_PASSWORD_SET, "Account has no password: " + name);
        }
        if (!PasswordHasher.matches(password, stored)) {
            throw new AuthException(Reason.WRONG_PASSWORD, "Wrong password");
        }
        return user;
    }

    private static AuthException userExists(String name) {
        return new AuthException(Reason.USER_EXISTS, "User already exists: " + name);
    }
}
