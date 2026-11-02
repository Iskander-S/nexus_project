package service;

import db.TaskRepository;
import db.TransactionRunner;
import model.Evaluatable;
import model.Task;
import model.User;

import java.util.List;
import java.util.Optional;

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

    /** Все задачи пользователя (и активные, и выполненные). */
    public List<Task> getTasks(String userId) {
        return taskRepository.findByUserId(userId);
    }

    public List<Task> getActiveTasks(String userId) {
        return getTasks(userId).stream()
                .filter(task -> !task.isCompleted())
                .toList();
    }

    public List<Task> getCompletedTasks(String userId) {
        return getTasks(userId).stream()
                .filter(Task::isCompleted)
                .toList();
    }

    /**
     * Удаляет задачу пользователя. Репозиторий удаляет по одному id и не знает о владельце,
     * поэтому принадлежность проверяется здесь: чужую задачу удалить нельзя.
     */
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

    /** Ищет активную задачу пользователя по ID (без учёта регистра и пробелов по краям). */
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

    /**
     * Завершает квест и начисляет XP.
     *
     * @param score оценка 0..100; обязательна для квестов с оценкой (Evaluatable), иначе игнорируется
     * @return сколько XP начислено
     */
    public int completeTask(String taskId, User user, Integer score) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID must not be empty");
        }
        Task task = findActiveTask(user.getId(), taskId)
                .orElseThrow(() -> new IllegalArgumentException("Active task not found: " + taskId.trim()));

        int xp = calculateXp(task, score); // task здесь — свежая копия из БД, её изменение в памяти безопасно

        // Работаем с копией профиля: настоящий user меняем только после успешного коммита
        User updated = new User(user.getId(), user.getName(), user.getLevel(), user.getCurrentXp(), user.getRank());
        updated.addXp(xp);

        // Задача и XP сохраняются атомарно: либо оба изменения, либо ни одного
        transactions.inTransaction(conn -> {
            taskRepository.updateTask(conn, task, user.getId());
            userService.updateProfile(conn, updated);
        });

        user.addXp(xp);
        return xp;
    }

    private int calculateXp(Task task, Integer score) {
        if (task instanceof Evaluatable) {
            if (score == null) {
                throw new IllegalArgumentException("Score (0-100) is required to complete this quest");
            }
            return evaluationService.processTaskEvaluation(task, score); // сам помечает задачу выполненной
        }
        task.complete();
        return task.getBaseXp();
    }
}
