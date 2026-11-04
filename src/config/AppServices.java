package config;

import db.AchievementRepository;
import db.DatabaseManager;
import db.H2AchievementRepositoryImpl;
import db.H2TaskRepositoryImpl;
import db.H2UserRepositoryImpl;
import db.TaskRepository;
import db.UserRepository;
import service.AchievementService;
import service.AuthService;
import service.AvatarService;
import service.LocalAvatarStorage;
import service.EvaluationService;
import service.QuestCompletionService;
import service.TaskService;
import service.UserService;

public record AppServices(UserService userService,
                          TaskService taskService,
                          AuthService authService,
                          AchievementService achievementService,
                          QuestCompletionService questCompletionService,
                          AvatarService avatarService) {

    public static AppServices create() {
        DatabaseManager.initTables();

        UserRepository userRepository = new H2UserRepositoryImpl();
        TaskRepository taskRepository = new H2TaskRepositoryImpl();
        AchievementRepository achievementRepository = new H2AchievementRepositoryImpl();

        UserService userService = new UserService(userRepository);
        TaskService taskService = new TaskService(
                taskRepository,
                userService,
                new EvaluationService(),
                DatabaseManager::inTransaction);
        AuthService authService = new AuthService(userRepository, taskService, DatabaseManager::inTransaction);

        AchievementService achievementService = new AchievementService(
                achievementRepository, taskRepository, AchievementService.defaultRules());
        achievementService.syncCatalog();
        QuestCompletionService questCompletionService = new QuestCompletionService(taskService, achievementService);

        AvatarService avatarService = new AvatarService(userRepository, new LocalAvatarStorage());

        return new AppServices(userService, taskService, authService, achievementService,
                questCompletionService, avatarService);
    }
}
