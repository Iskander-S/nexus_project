package db;

import model.Task;

import java.sql.Connection;
import java.util.List;

public interface TaskRepository extends CrudRepository<Task, String> {
    List<Task> findByUserId(String userId);

    /** Создаёт задачу. Если задача с таким id уже есть — DataAccessException, а не тихая перезапись. */
    void insertTask(Task task, String userId);

    /** Обновляет СВОЮ задачу в рамках транзакции. Если задача не найдена или чужая — DataAccessException. */
    void updateTask(Connection conn, Task task, String userId);
}
