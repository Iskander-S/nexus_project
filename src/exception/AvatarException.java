package exception;

public class AvatarException extends RuntimeException {
    public AvatarException(String message) {
        super(message);
    }

    public AvatarException(String message, Throwable cause) {
        super(message, cause);
    }
}
