package ui;

import model.DailyTask;
import model.Task;
import model.User;
import service.TaskService;
import service.UserService;

import java.util.List;
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
            String choice = scanner.nextLine();

            try {
                running = processChoice(choice);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        System.out.println("System shutting down...");
    }

    private void printMenu() {
        System.out.println("\n1. View Profile");
        System.out.println("2. View Active Quests");
        System.out.println("3. Add Daily Quest");
        System.out.println("4. Complete Quest");
        System.out.println("5. Exit");
        System.out.print("Select action: ");
    }

    private boolean processChoice(String choice) {
        switch (choice) {
            case "1" -> {
                System.out.printf("Profile: %s | Lvl: %d | XP: %d | Rank: %s%n",
                        currentUser.getName(), currentUser.getLevel(), currentUser.getCurrentXp(), currentUser.getRank());
            }
            case "2" -> {
                List<Task> tasks = taskService.getActiveTasks(currentUser.getId());
                if (tasks.isEmpty()) System.out.println("No active quests.");
                tasks.forEach(t -> System.out.printf("[%s] %s (XP: %d)%n", t.getId(), t.getTitle(), t.getBaseXp()));
            }
            case "3" -> {
                System.out.print("Enter quest title: ");
                String title = scanner.nextLine();
                System.out.print("Enter base XP (e.g. 100): ");
                int xp = Integer.parseInt(scanner.nextLine());

                String taskId = "T-" + UUID.randomUUID().toString().substring(0, 5);
                Task newTask = new DailyTask(taskId, title, "Generated task", xp);
                taskService.assignTask(newTask, currentUser.getId());
                System.out.println("Quest added!");
            }
            case "4" -> {
                System.out.print("Enter Quest ID to complete: ");
                String taskId = scanner.nextLine();
                taskService.completeTask(taskId, currentUser);
                System.out.println("Quest completed! XP awarded.");
            }
            case "5" -> { return false; }
            default -> System.out.println("Invalid choice.");
        }
        return true;
    }
}