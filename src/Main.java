import config.AppServices;
import db.DatabaseManager;
import javafx.application.Application;
import ui.AppRunner;
import ui.NexusApp;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            runCli();
        } else {
            Application.launch(NexusApp.class, args);
        }
    }

    private static void runCli() {
        try {
            AppServices services = AppServices.create();
            new AppRunner(services.userService(), services.taskService()).run();
        } finally {
            DatabaseManager.shutdown();
        }
    }
}
