package ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.stage.Window;
import model.User;
import service.TaskService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class AppContext {
    private final TaskService taskService;
    private final User user;
    private final Window window;
    private final ToastHost toasts = new ToastHost();
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nexus-db");
        thread.setDaemon(true);
        return thread;
    });

    private int pending;
    private Runnable reloadAction = () -> { };

    public AppContext(TaskService taskService, User user, Window window) {
        this.taskService = taskService;
        this.user = user;
        this.window = window;
    }

    public TaskService tasks() { return taskService; }
    public User user() { return user; }
    public Window window() { return window; }
    public ToastHost toasts() { return toasts; }
    public BooleanProperty busyProperty() { return busy; }

    public void setReloadAction(Runnable reloadAction) {
        this.reloadAction = reloadAction;
    }

    public void reload() {
        reloadAction.run();
    }

    public void toast(String message, ToastHost.Kind kind) {
        toasts.show(message, kind);
    }

    public <T> void async(Callable<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        pending++;
        busy.set(true);

        task.setOnSucceeded(e -> {
            finishOne();
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            finishOne();
            Throwable error = task.getException();
            String message = error == null || error.getMessage() == null
                    ? "Something went wrong." : error.getMessage();
            toast(message, ToastHost.Kind.ERROR);
        });
        executor.execute(task);
    }

    private void finishOne() {
        pending--;
        busy.set(pending > 0);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
