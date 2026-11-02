package service;

import db.TaskRepository;
import model.Task;
import model.User;

import java.util.List;

public class TaskService {
    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }

    public void assignTask(Task task, String userId) {
        if (task.getBaseXp() <= 0) {
            throw new IllegalArgumentException("Base XP must be positive: " + task.getBaseXp());
        }
        taskRepository.saveTaskWithUserId(task, userId);
    }

    public List<Task> getActiveTasks(String userId) {
        return taskRepository.findByUserId(userId).stream()
                .filter(task -> !task.isCompleted())
                .toList();
    }

    public void completeTask(String taskId, User user) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID must not be empty");
        }
        String wantedId = taskId.trim();
        List<Task> userTasks = taskRepository.findByUserId(user.getId());

        Task taskToComplete = userTasks.stream()
                .filter(t -> t.getId().equalsIgnoreCase(wantedId) && !t.isCompleted())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Active task not found: " + wantedId));

        taskToComplete.setCompleted(true);
        taskRepository.saveTaskWithUserId(taskToComplete, user.getId());

        user.addXp(taskToComplete.getBaseXp());
        userService.updateProfile(user);
    }
}