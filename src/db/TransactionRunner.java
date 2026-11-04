package db;

@FunctionalInterface
public interface TransactionRunner {
    void inTransaction(TransactionWork work);
}
