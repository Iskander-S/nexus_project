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

    /**
     * Пул создаётся лениво, а не в static-блоке. Раньше при ошибке подключения (например, файл БД занят)
     * класс «ломался» навсегда: ExceptionInInitializerError, а затем NoClassDefFoundError в shutdown(),
     * который скрывал настоящую причину.
     */
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
                rank VARCHAR(20) DEFAULT 'E_RANK'
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
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;
        // по одному запросу на execute() — не зависим от поддержки multi-statement драйвером
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(users);
            stmt.execute(tasks);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize database tables", e);
        }
    }

    /**
     * Выполняет work в одной транзакции: commit при успехе, rollback при любой ошибке.
     * Исключения DataAccessException пробрасываются как есть.
     */
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
        // не создаём пул ради того, чтобы тут же его закрыть
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
