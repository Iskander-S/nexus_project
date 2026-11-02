package config;

import db.DatabaseManager;
import db.H2TaskRepositoryImpl;
import db.H2UserRepositoryImpl;
import service.EvaluationService;
import service.TaskService;
import service.UserService;

/** Единое место сборки зависимостей — им пользуются и GUI, и CLI. */
public record AppServices(UserService userService, TaskService taskService) {

    public static AppServices create() {
        DatabaseManager.initTables();

        UserService userService = new UserService(new H2UserRepositoryImpl());
        TaskService taskService = new TaskService(
                new H2TaskRepositoryImpl(),
                userService,
                new EvaluationService(),
                DatabaseManager::inTransaction);
        return new AppServices(userService, taskService);
    }
}
