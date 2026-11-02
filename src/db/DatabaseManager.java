package db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import exception.DataAccessException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static final HikariDataSource dataSource;

    static {
        Properties props = new Properties();
        try (InputStream input = DatabaseManager.class.getResourceAsStream("/application.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                throw new RuntimeException("application.properties not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DB config", e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.user"));
        config.setPassword(props.getProperty("db.password"));
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max-size", "10")));

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void initTables() {
        String schema = """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(50) PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                level INT DEFAULT 1,
                current_xp INT DEFAULT 0,
                rank VARCHAR(20) DEFAULT 'E_RANK'
            );
            CREATE TABLE IF NOT EXISTS tasks (
                id VARCHAR(50) PRIMARY KEY,
                user_id VARCHAR(50) NOT NULL,
                task_type VARCHAR(20) NOT NULL,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                base_xp INT NOT NULL,
                completed BOOLEAN DEFAULT FALSE,
                target_score INT,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
        """;
        try (Connection conn = getConnection(); var stmt = conn.createStatement()) {
            stmt.execute(schema);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize database tables", e);
        }
    }
}