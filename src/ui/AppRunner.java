package ui;

import model.DailyTask;
import model.Evaluatable;
import model.GlobalQuest;
import model.Task;
import model.User;
import model.WeeklyTask;
import service.TaskService;
import service.UserService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class AppRunner {
    private final UserService userService;
    private final TaskService taskService;
    private final Scanner scanner;
    private User currentUser;

    public AppRunner(UserService userService, TaskService taskService) {
        this.userService = userService;
        this.taskService = taskService;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        System.out.println("=== NEXUS SYSTEM CLI ===");
        currentUser = userService.getOrCreateUser("user_001", "Iskander");

        boolean running = true;
        while (running) {
            printMenu();
            if (!scanner.hasNextLine()) {
                break;
            }
            String choice = scanner.nextLine().trim();

            try {
                running = processChoice(choice);
            } catch (NoSuchElementException e) {
                break;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        System.out.println("System shutting down...");
    }

    private void printMenu() {
        System.out.println("\n1. View Profile");
        System.out.println("2. View Active Quests");
        System.out.println("3. Add Quest");
        System.out.println("4. Complete Quest");
        System.out.println("5. Exit");
        System.out.print("Select action: ");
    }

    private boolean processChoice(String choice) {
        switch (choice) {
            case "1" -> System.out.printf("Profile: %s | Lvl: %d | XP: %d | Rank: %s%n",
                    currentUser.getName(), currentUser.getLevel(), currentUser.getCurrentXp(),
                    currentUser.getRank().getDisplayName());
            case "2" -> printActiveQuests();
            case "3" -> addQuest();
            case "4" -> completeQuest();
            case "5" -> {
                return false;
            }
            default -> System.out.println("Invalid choice.");
        }
        return true;
    }

    private void printActiveQuests() {
        List<Task> tasks = taskService.getActiveTasks(currentUser.getId());
        if (tasks.isEmpty()) System.out.println("No active quests.");
        tasks.forEach(t -> System.out.printf("[%s] %-7s %s (XP: %d)%n",
                t.getId(), typeLabel(t), t.getTitle(), t.getBaseXp()));
    }

    private void addQuest() {
        System.out.print("Type (1 - Daily, 2 - Weekly, 3 - Global): ");
        String type = scanner.nextLine().trim();
        if (!List.of("1", "2", "3").contains(type)) {
            throw new IllegalArgumentException("Unknown quest type.");
        }

        System.out.print("Enter quest title: ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Quest title must not be empty.");
        }
        int xp = readInt("Enter base XP (e.g. 100): ", "XP");
        if (xp <= 0) {
            throw new IllegalArgumentException("Base XP must be positive.");
        }

        // 5 hex-символов давали реальные коллизии; теперь 8, а БД при дубликате вернёт ошибку, а не перезапишет задачу
        String id = "T-" + UUID.randomUUID().toString().substring(0, 8);
        Task task = switch (type) {
            case "1" -> new DailyTask(id, title, "Generated task", xp);
            case "2" -> new WeeklyTask(id, title, "Generated task", xp);
            default -> {
                int target = readInt("Enter target score (1-100): ", "Target score");
                yield new GlobalQuest(id, title, "Generated task", xp, target);
            }
        };

        taskService.assignTask(task, currentUser.getId());
        System.out.println("Quest added! ID: " + id);
    }

    private void completeQuest() {
        System.out.print("Enter Quest ID to complete: ");
        String taskId = scanner.nextLine().trim();

        Integer score = null;
        Optional<Task> task = taskService.findActiveTask(currentUser.getId(), taskId);
        if (task.isPresent() && task.get() instanceof Evaluatable) {
            score = readInt("Enter your score (0-100): ", "Score");
        }

        int xp = taskService.completeTask(taskId, currentUser, score);
        System.out.printf("Quest completed! +%d XP%n", xp);
    }

    private int readInt(String prompt, String field) {
        System.out.print(prompt);
        String raw = scanner.nextLine().trim();
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a whole number.");
        }
    }

    private static String typeLabel(Task t) {
        if (t instanceof GlobalQuest) return "GLOBAL";
        if (t instanceof WeeklyTask) return "WEEKLY";
        return "DAILY";
    }
}
