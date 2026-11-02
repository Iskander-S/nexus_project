package db;

import model.User;

import java.sql.Connection;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, String> {
    /** Upsert пользователя (создать или обновить). */
    User save(User user);

    /** То же, но в рамках уже открытой транзакции. */
    User save(Connection conn, User user);

    Optional<User> findByName(String name);
}
