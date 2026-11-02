import db.DatabaseManager;
import db.H2TaskRepositoryImpl;
import db.H2UserRepositoryImpl;
import service.TaskService;
import service.UserService;
import ui.AppRunner;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager.initTables();

            UserService userService = new UserService(new H2UserRepositoryImpl());
            TaskService taskService = new TaskService(new H2TaskRepositoryImpl(), userService);

            new AppRunner(userService, taskService).run();
        } finally {
            DatabaseManager.shutdown();
        }
    }
}
