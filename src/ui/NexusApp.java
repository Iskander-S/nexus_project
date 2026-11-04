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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
    private AppServices services;
    private Scene scene;

    private AppContext ctx;
    private DashboardView dashboard;
    private QuestsView quests;
    private HistoryView history;
    private StackPane pages;
    private StackPane sidebarAvatar;
    private Label sidebarName;
    private Label sidebarMeta;
    private Label topUser;

    @Override
    public void start(Stage stage) {
        try {
            services = AppServices.create();
        } catch (Throwable t) {
            fatal(t);
            return;
        }

        scene = new Scene(new StackPane(), 1180, 760);
        ThemeManager.apply(scene);

        stage.setTitle("Nexus");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);

        showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        endSession();
        DatabaseManager.shutdown();
    }

    private void showLogin() {
        endSession();
        scene.getAccelerators().clear();
        scene.setRoot(new LoginView(services.authService(), this::startSession, this::toggleTheme));
    }

    private void startSession(User user, boolean justRegistered) {
        scene.setRoot(buildMain(user));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                new QuestActions(ctx)::addQuest);
        ctx.reload();
        if (justRegistered) {
            ctx.toast("Welcome, " + user.getName() + "! We have prepared starter quests for you.",
                    NotificationService.Kind.SUCCESS);
        }
    }

    private void endSession() {
        if (ctx != null) {
            ctx.shutdown();
            ctx = null;
        }
    }

    private void toggleTheme() {
        ThemeManager.toggle(scene);
    }

    private Region buildMain(User user) {
        ctx = new AppContext(services, user, scene.getWindow());
        QuestActions actions = new QuestActions(ctx);
        ProfileActions profileActions = new ProfileActions(ctx);

        dashboard = new DashboardView(actions, profileActions::editProfile);
        quests = new QuestsView(actions);
        history = new HistoryView();
        pages = new StackPane(dashboard, quests, history);
        sidebarAvatar = new StackPane();
        sidebarName = Ui.label("", "side-name");
        sidebarMeta = Ui.label("", "side-meta");
        topUser = Ui.label("", "top-user");

        ToggleGroup nav = new ToggleGroup();
        ToggleButton dashboardTab = navButton("Dashboard", Ui.icon(Ui.ICON_DASHBOARD, 18, 18), nav, dashboard);
        ToggleButton questsTab = navButton("Quests", Ui.icon(Ui.ICON_QUESTS, 16, 14), nav, quests);
        ToggleButton historyTab = navButton("History", Ui.icon(Ui.ICON_CHECK, 18, 14), nav, history);
        nav.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                nav.selectToggle(oldToggle);
            }
        });
        dashboardTab.setSelected(true);

        Region blocker = new Region();
        blocker.setCursor(Cursor.WAIT);
        blocker.visibleProperty().bind(ctx.busyProperty());

        ToastHost toasts = ctx.toasts();
        StackPane.setAlignment(toasts, Pos.TOP_RIGHT);
        StackPane.setMargin(toasts, new Insets(20));
        toasts.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane body = new StackPane(pages, blocker, toasts);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox content = new VBox(buildTopBar(), body);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox shell = new HBox(buildSidebar(profileActions, dashboardTab, questsTab, historyTab), content);
        shell.getStyleClass().add("shell");

        ctx.setReloadAction(this::reload);
        return shell;
    }

    private HBox buildTopBar() {
        topUser.setText("Signed in as " + ctx.user().getName());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button theme = new Button("Toggle Theme", Ui.icon(Ui.ICON_THEME, 16, 16));
        theme.getStyleClass().add("ghost");
        theme.setOnAction(e -> toggleTheme());

        HBox bar = new HBox(12, topUser, spacer, theme);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private VBox buildSidebar(ProfileActions profileActions, ToggleButton... tabs) {
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
        profile.setCursor(Cursor.HAND);
        profile.setOnMouseClicked(e -> profileActions.editProfile());
        Tooltip.install(profile, new Tooltip("Edit profile"));

        Button logout = new Button("Log out", Ui.icon(Ui.ICON_LOGOUT, 16, 16));
        logout.getStyleClass().addAll("ghost", "logout-button");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setAlignment(Pos.CENTER_LEFT);
        logout.setOnAction(e -> showLogin());
        VBox.setMargin(logout, new Insets(10, 0, 0, 0));

        VBox sidebar = new VBox(brand, nav, spacer, profile, logout);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setAlignment(Pos.TOP_LEFT);
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
        AppContext session = ctx;
        if (session == null) {
            return;
        }
        session.async(() -> new Snapshot(
                session.user(),
                session.tasks().getTasks(session.user().getId()),
                session.users().getLeaderboard(UserService.LEADERBOARD_SIZE),
                session.achievements().getUnlocked(session.user().getId())), this::render);
    }

    private void render(Snapshot snapshot) {
        if (ctx == null) {
            return;
        }
        dashboard.render(snapshot);
        quests.render(snapshot);
        history.render(snapshot);

        User user = snapshot.user();
        sidebarAvatar.getChildren().setAll(Ui.avatar(user, 40));
        sidebarName.setText(user.getName());
        sidebarMeta.setText("Level " + user.getLevel() + "  ·  " + user.getRank().getDisplayName());
        topUser.setText("Signed in as " + user.getName());
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
