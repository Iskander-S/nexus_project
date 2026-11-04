package exception;

public class AuthException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Reason {
        INVALID_USERNAME, WEAK_PASSWORD, USER_EXISTS, USER_NOT_FOUND, WRONG_PASSWORD, NO_PASSWORD_SET
    }

    private final Reason reason;

    public AuthException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
