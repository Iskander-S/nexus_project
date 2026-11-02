package db;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface TransactionWork {
    void execute(Connection conn) throws SQLException;
}
