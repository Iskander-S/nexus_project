package db;

import model.Task;

import java.sql.Connection;
import java.util.List;

public interface TaskRepository extends CrudRepository<Task, String> {
    List<Task> findByUserId(String userId);

    void insertTask(Task task, String userId);

    void insertTask(Connection conn, Task task, String userId);

    void updateTask(Connection conn, Task task, String userId);
}
