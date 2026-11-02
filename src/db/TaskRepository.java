package db;

import model.Task;
import java.util.List;

public interface TaskRepository extends CrudRepository<Task, String> {
    List<Task> findByUserId(String userId);
    void saveTaskWithUserId(Task task, String userId);
    void saveAll(List<Task> tasks, String userId);
}