import db.DatabaseManager;
import db.H2TaskRepositoryImpl;
import db.H2UserRepositoryImpl;
import service.EvaluationService;
import service.TaskService;
import service.UserService;
import ui.AppRunner;

public class пше Main {
    public static void main(String[] args) {
        try {
            DatabaseManager.initTables();

            UserService userService = new UserService(new H2UserRepositoryImpl());
            TaskService taskService = new TaskService(
                    new H2TaskRepositoryImpl(),
                    userService,
                    new EvaluationService(),
                    DatabaseManager::inTransaction);

            new AppRunner(userService, taskService).run();
        } finally {
            // корректно закрываем пул, чтобы H2 сбросил данные и снял блокировку файла
            DatabaseManager.shutdown();
        }
    }
}
