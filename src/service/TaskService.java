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
        taskRepository.saveTaskWithUserId(task, userId);
    }

    public List<Task> getActiveTasks(String userId) {
        return taskRepository.findByUserId(userId).stream()
                .filter(task -> !task.isCompleted())
                .toList();
    }

    public void completeTask(String taskId, User user) {
        List<Task> userTasks = taskRepository.findByUserId(user.getId());

        Task taskToComplete = userTasks.stream()
                .filter(t -> t.getId().equals(taskId) && !t.isCompleted())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Active task not found: " + taskId));

        taskToComplete.setCompleted(true);
        user.addXp(taskToComplete.getBaseXp());

        taskRepository.saveTaskWithUserId(taskToComplete, user.getId());
        userService.updateProfile(user);
    }
}