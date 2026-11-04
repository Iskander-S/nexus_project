package db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseManager {
    private static HikariDataSource dataSource;

    private DatabaseManager() {
    }

    private static synchronized HikariDataSource dataSource() {
        if (dataSource == null) {
            dataSource = createDataSource();
        }
        return dataSource;
    }

    private static HikariDataSource createDataSource() {
        Properties props = new Properties();
        try (InputStream input = DatabaseManager.class.getResourceAsStream("/application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties not found on classpath");
            }
            props.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load DB config", e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getProperty("nexus.db.url", props.getProperty("db.url")));
        config.setUsername(props.getProperty("db.user"));
        config.setPassword(props.getProperty("db.password"));
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max-size", "10")));

        try {
            return new HikariDataSource(config);
        } catch (RuntimeException e) {
            throw new DataAccessException("Cannot open database " + config.getJdbcUrl() + ": " + rootMessage(e), e);
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    public static void initTables() {
        String users = """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(50) PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                level INT DEFAULT 1,
                current_xp INT DEFAULT 0,
                rank VARCHAR(20) DEFAULT 'E_RANK',
                password_hash VARCHAR(128),
                avatar_path VARCHAR(500)
            )
            """;
        String tasks = """
            CREATE TABLE IF NOT EXISTS tasks (
                id VARCHAR(50) PRIMARY KEY,
                user_id VARCHAR(50) NOT NULL,
                task_type VARCHAR(20) NOT NULL,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                base_xp INT NOT NULL,
                completed BOOLEAN DEFAULT FALSE,
                target_score INT,
                deadline DATE,
                grade VARCHAR(2),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;
        String achievements = """
            CREATE TABLE IF NOT EXISTS achievements (
                id VARCHAR(50) PRIMARY KEY,
                title VARCHAR(100) NOT NULL,
                description VARCHAR(255),
                icon VARCHAR(20)
            )
            """;
        String userAchievements = """
            CREATE TABLE IF NOT EXISTS user_achievements (
                user_id VARCHAR(50) NOT NULL,
                achievement_id VARCHAR(50) NOT NULL,
                unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                PRIMARY KEY (user_id, achievement_id),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
            )
            """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(users);
            stmt.execute(tasks);
            stmt.execute(achievements);
            stmt.execute(userAchievements);
            stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(128)");
            stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_path VARCHAR(500)");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS deadline DATE");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS grade VARCHAR(2)");
            try {
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_users_name ON users(name)");
            } catch (SQLException e) {
                System.err.println("Warning: unique index on users.name was not created: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize database tables", e);
        }
    }

    public static void inTransaction(TransactionWork work) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                work.execute(conn);
                conn.commit();
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new DataAccessException("Transaction failed", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error during transaction", e);
        }
    }

    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
