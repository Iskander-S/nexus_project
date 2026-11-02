package db;

import java.util.List;
import java.util.Optional;

/**
 * Общие операции чтения/удаления. save() намеренно НЕ входит сюда:
 * для разных сущностей он выглядит по-разному (задачу нельзя сохранить без userId).
 */
public interface CrudRepository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean deleteById(ID id);
}
