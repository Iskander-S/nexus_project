package service;

import db.TaskRepository;
import db.TransactionRunner;
import model.DailyTask;
import model.GlobalQuest;
import model.Task;
import model.User;
import model.WeeklyTask;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TaskService {
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final EvaluationService evaluationService;
    private final TransactionRunner transactions;

    public TaskService(TaskRepository taskRepository,
                       UserService userService,
                       EvaluationService evaluationService,
                       TransactionRunner transactions) {
        this.taskRepository = taskRepository;
        this.userService = userService;
        this.evaluationService = evaluationService;
        this.transactions = transactions;
    }

    public void assignTask(Task task, String userId) {
        if (task == null) {
            throw new IllegalArgumentException("Task must not be null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID must not be empty");
        }
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Quest title must not be empty");
        }
        if (task.getBaseXp() <= 0) {
            throw new IllegalArgumentException("Base XP must be positive: " + task.getBaseXp());
        }
        taskRepository.insertTask(task, userId);
    }

    public List<Task> assignStarterQuests(Connection conn, String userId) {
        List<Task> quests = buildStarterQuests();
        for (Task quest : quests) {
            taskRepository.insertTask(conn, quest, userId);
        }
        return quests;
    }

    public static List<Task> buildStarterQuests() {
        LocalDate today = LocalDate.now();
        return List.of(
                withDeadline(new DailyTask(newId(), "Setup University Email",
                        "Activate your student mailbox and read the welcome letter.", 60), today.plusDays(1)),
                withDeadline(new DailyTask(newId(), "Find Campus Cafeteria",
                        "Locate the cafeteria and its opening hours.", 50), today.plusDays(2)),
                withDeadline(new WeeklyTask(newId(), "Get Library Card",
                        "Visit the library desk with your student ID and get a reader's card.", 100), today.plusDays(7)),
                withDeadline(new GlobalQuest(newId(), "Pass the Orientation Quiz",
                        "Finish the first-year orientation quiz. Enter your result as a score.", 200, 80),
                        today.plusDays(14)));
    }

    private static Task withDeadline(Task task, LocalDate deadline) {
        task.setDeadline(deadline);
        return task;
    }

    private static String newId() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public List<Task> getTasks(String userId) {
        return taskRepository.findByUserId(userId);
    }

    public List<Task> getActiveTasks(String userId) {
        return getTasks(userId).stream()
                .filter(task -> !task.isCompleted())
                .sorted(Task.DEADLINE_ORDER)
                .toList();
    }

    public List<Task> getCompletedTasks(String userId) {
        return getTasks(userId).stream()
                .filter(Task::isCompleted)
                .toList();
    }

    public void deleteTask(String userId, String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID must not be empty");
        }
        String wantedId = taskId.trim();
        Task task = getTasks(userId).stream()
                .filter(t -> t.getId().equalsIgnoreCase(wantedId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + wantedId));
        taskRepository.deleteById(task.getId());
    }

    public Optional<Task> findActiveTask(String userId, String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return Optional.empty();
        }
        String wantedId = taskId.trim();
        return getActiveTasks(userId).stream()
                .filter(t -> t.getId().equalsIgnoreCase(wantedId))
                .findFirst();
    }

    public int completeTask(String taskId, User user) {
        return completeTask(taskId, user, null);
    }

    public int completeTask(String taskId, User user, Integer score) {
        return complete(taskId, user, score).xp();
    }

    public CompletionResult complete(String taskId, User user, Integer score) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID must not be empty");
        }
        Task task = findActiveTask(user.getId(), taskId)
                .orElseThrow(() -> new IllegalArgumentException("Active task not found: " + taskId.trim()));
        if (score == null) {
            throw new IllegalArgumentException("Score (0-100) is required to complete this quest");
        }

        EvaluationResult evaluation = evaluationService.evaluate(task, score);
        int xp = evaluation.totalXp();

        User updated = new User(user.getId(), user.getName(), user.getLevel(), user.getCurrentXp(), user.getRank(),
                user.getAvatarPath());
        updated.addXp(xp);

        transactions.inTransaction(conn -> {
            taskRepository.updateTask(conn, task, user.getId());
            userService.updateProfile(conn, updated);
        });

        user.addXp(xp);
        return new CompletionResult(xp, evaluation.grade(), List.of());
    }
}
