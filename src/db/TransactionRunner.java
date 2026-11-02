package db;

/** Запускает работу в одной транзакции: либо все записи применяются, либо ни одна. */
@FunctionalInterface
public interface TransactionRunner {
    void inTransaction(TransactionWork work);
}
