package ui;

import config.AppServices;
import db.DatabaseManager;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.User;
import service.UserService;

public class NexusApp extends Application {
    private static final String USER_ID = "user_001";
    private static final String DEFAULT_NAME = "Iskander";

    private AppContext ctx;
    private DashboardView dashboard;
    private QuestsView quests;
    private HistoryView history;
    private final StackPane pages = new StackPane();
    private final StackPane sidebarAvatar = new StackPane();
    private final Label sidebarName = Ui.label("", "side-name");
    private final Label sidebarMeta = Ui.label("", "side-meta");

    @Override
    public void start(Stage stage) {
        AppServices services;
        User user;
        try {
            services = AppServices.create();
            user = loadOrCreateUser(services.userService());
        } catch (Throwable t) {
            fatal(t);
            return;
        }

        ctx = new AppContext(services.taskService(), user, stage);
        QuestActions actions = new QuestActions(ctx);
        dashboard = new DashboardView(actions);
        quests = new QuestsView(actions);
        history = new HistoryView();
        pages.getChildren().addAll(dashboard, quests, history);

        ToggleGroup nav = new ToggleGroup();
        ToggleButton dashboardTab = navButton("Dashboard", Ui.icon(Ui.ICON_DASHBOARD, 18, 18), nav, dashboard);
        ToggleButton questsTab = navButton("Quests", Ui.icon(Ui.ICON_QUESTS, 16, 14), nav, quests);
        ToggleButton historyTab = navButton("History", Ui.icon(Ui.ICON_CHECK, 18, 14), nav, history);
        nav.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {          // нельзя снять выбор: всегда открыт какой-то экран
                nav.selectToggle(oldToggle);
            }
        });
        dashboardTab.setSelected(true);

        VBox sidebar = buildSidebar(dashboardTab, questsTab, historyTab);

        Region blocker = new Region();
        blocker.setCursor(Cursor.WAIT);
        blocker.visibleProperty().bind(ctx.busyProperty());

        ToastHost toasts = ctx.toasts();
        StackPane.setAlignment(toasts, Pos.TOP_RIGHT);
        StackPane.setMargin(toasts, new Insets(20));
        toasts.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane content = new StackPane(pages, blocker, toasts);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox shell = new HBox(sidebar, content);
        shell.getStyleClass().add("shell");

        Scene scene = new Scene(shell, 1180, 760);
        scene.getStylesheets().add(Ui.STYLESHEET);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), actions::addQuest);

        stage.setTitle("Nexus");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);

        ctx.setReloadAction(this::reload);
        stage.show();
        reload();
    }

    @Override
    public void stop() {
        if (ctx != null) {
            ctx.shutdown();
        }
        DatabaseManager.shutdown();
    }

    private User loadOrCreateUser(UserService userService) {
        return userService.findUser(USER_ID).orElseGet(() -> {
            String name = Dialogs.askName(DEFAULT_NAME);
            return userService.getOrCreateUser(USER_ID, name);
        });
    }

    private VBox buildSidebar(ToggleButton... tabs) {
        Region brandIcon = Ui.icon(Ui.ICON_BRAND, 24, 24);
        brandIcon.getStyleClass().add("brand-icon");
        VBox brandText = new VBox(0, Ui.label("NEXUS", "brand-title"), Ui.label("QUEST SYSTEM", "brand-subtitle"));
        HBox brand = new HBox(12, brandIcon, brandText);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(4, 8, 22, 8));

        VBox nav = new VBox(6, tabs);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox profileText = new VBox(2, sidebarName, sidebarMeta);
        HBox profile = new HBox(12, sidebarAvatar, profileText);
        profile.setAlignment(Pos.CENTER_LEFT);
        profile.getStyleClass().add("side-profile");

        VBox sidebar = new VBox(brand, nav, spacer, profile);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(24, 16, 16, 16));
        sidebar.setPrefWidth(250);
        sidebar.setMinWidth(250);
        sidebar.setMaxWidth(250);
        return sidebar;
    }

    private ToggleButton navButton(String text, Node icon, ToggleGroup group, Node page) {
        ToggleButton button = new ToggleButton(text, icon);
        button.setToggleGroup(group);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.selectedProperty().addListener((obs, was, isSelected) -> {
            if (isSelected) {
                showPage(page);
            }
        });
        return button;
    }

    private void showPage(Node page) {
        for (Node p : pages.getChildren()) {
            boolean visible = p == page;
            p.setVisible(visible);
            p.setManaged(visible);
        }
        FadeTransition fade = new FadeTransition(Duration.millis(220), page);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }


    private void reload() {
        ctx.async(() -> new Snapshot(ctx.user(), ctx.tasks().getTasks(ctx.user().getId())), this::render);
    }

    private void render(Snapshot snapshot) {
        dashboard.render(snapshot);
        quests.render(snapshot);
        history.render(snapshot);

        User user = snapshot.user();
        sidebarAvatar.getChildren().setAll(Ui.avatar(user.getName(), 40, user.getRank()));
        sidebarName.setText(user.getName());
        sidebarMeta.setText("Level " + user.getLevel() + "  ·  " + user.getRank().getDisplayName());
    }

    private void fatal(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String details = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
        Dialogs.error(null, "Nexus could not start",
                error.getMessage() != null && !error.getMessage().equals(details)
                        ? error.getMessage() + "\n\n" + details
                        : details);
        Platform.exit();
    }
}
