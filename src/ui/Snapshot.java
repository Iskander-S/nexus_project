package ui;

import model.Achievement;
import model.Task;
import model.User;

import java.util.List;

public record Snapshot(User user, List<Task> tasks, List<User> leaderboard, List<Achievement> achievements) {

    public List<Task> active() {
        return tasks.stream()
                .filter(t -> !t.isCompleted())
                .sorted(Task.DEADLINE_ORDER)
                .toList();
    }

    public List<Task> completed() {
        return tasks.stream().filter(Task::isCompleted).toList();
    }
}
