package db;

import model.User;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, String> {
    User save(User user);

    User save(Connection conn, User user);

    Optional<User> findByName(String name);

    Optional<String> findPasswordHash(String userId);

    void setPasswordHash(Connection conn, String userId, String passwordHash);

    List<User> findTopByXp(int limit);
}
