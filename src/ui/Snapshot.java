package ui;

import model.Task;
import model.User;

import java.util.List;

public record Snapshot(User user, List<Task> tasks) {

    public List<Task> active() {
        return tasks.stream().filter(t -> !t.isCompleted()).toList();
    }

    public List<Task> completed() {
        return tasks.stream().filter(Task::isCompleted).toList();
    }
}
