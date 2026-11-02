package service;

import exception.TaskNotFoundException;
import model.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TaskManager {
    private final List<Task> taskList;

    public TaskManager() {
        this.taskList = new ArrayList<>();
    }

    public TaskManager(List<Task> initialTasks) {
        this.taskList = initialTasks != null ? new ArrayList<>(initialTasks) : new ArrayList<>();
    }

    public void addTask(Task task) {
        taskList.add(task);
    }

    public Task getTaskById(String id) throws TaskNotFoundException {
        return taskList.stream()
                .filter(t -> t.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new TaskNotFoundException("Task with ID '" + id + "' not found."));
    }

    public List<Task> getPendingTasks() {
        return taskList.stream()
                .filter(t -> !t.isCompleted())
                .collect(Collectors.toList());
    }

    public List<Task> getCompletedTasks() {
        return taskList.stream()
                .filter(Task::isCompleted)
                .collect(Collectors.toList());
    }

    public <T extends Task> List<T> getTasksByType(Class<T> type) {
        return taskList.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public List<Task> getTasksSortedByXpDesc() {
        return taskList.stream()
                .sorted(Comparator.comparingInt(Task::getBaseXp).reversed())
                .collect(Collectors.toList());
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(taskList);
    }
}