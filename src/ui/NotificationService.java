package ui;

public interface NotificationService {

    enum Kind { INFO, SUCCESS, ERROR, LEVEL_UP, ACHIEVEMENT }

    void show(String message, Kind kind);
}
