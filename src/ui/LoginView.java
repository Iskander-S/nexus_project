package ui;

import exception.AuthException;
import exception.DataAccessException;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.User;
import service.AuthService;

import java.util.function.BiConsumer;

public final class LoginView extends StackPane {
    private final AuthService auth;
    private final BiConsumer<User, Boolean> onSuccess;

    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final PasswordField confirm = new PasswordField();
    private final VBox confirmBox;
    private final Button submit = new Button();
    private boolean registerMode;

    public LoginView(AuthService auth, BiConsumer<User, Boolean> onSuccess, Runnable onToggleTheme) {
        this.auth = auth;
        this.onSuccess = onSuccess;

        ToggleGroup mode = new ToggleGroup();
        ToggleButton signIn = modeButton("Sign in", mode);
        ToggleButton signUp = modeButton("Sign up", mode);
        HBox modes = new HBox(8, signIn, signUp);
        mode.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                mode.selectToggle(oldToggle);
                return;
            }
            setRegisterMode(newToggle == signUp);
        });

        username.setPromptText("Username");
        password.setPromptText("Password");
        confirm.setPromptText("Repeat password");
        confirmBox = field("Confirm password", confirm);
        confirmBox.setVisible(false);
        confirmBox.setManaged(false);

        username.setOnAction(e -> password.requestFocus());
        password.setOnAction(e -> {
            if (registerMode) confirm.requestFocus(); else submit();
        });
        confirm.setOnAction(e -> submit());

        submit.getStyleClass().add("primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setDefaultButton(true);
        submit.setOnAction(e -> submit());

        Label subtitle = Ui.label("Quest system for first-year students", "login-subtitle");
        VBox card = new VBox(16,
                Ui.label("Nexus", "login-title"),
                subtitle,
                modes,
                field("Username", username),
                field("Password", password),
                confirmBox,
                submit);
        card.getStyleClass().addAll("card", "login-card");
        card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Button theme = new Button("Toggle Theme", Ui.icon(Ui.ICON_THEME, 16, 16));
        theme.getStyleClass().add("ghost");
        theme.setOnAction(e -> onToggleTheme.run());
        StackPane.setAlignment(theme, Pos.TOP_RIGHT);
        StackPane.setMargin(theme, new Insets(18));

        getChildren().addAll(card, theme);
        getStyleClass().add("login-view");
        signIn.setSelected(true);
        setRegisterMode(false);
    }

    private void setRegisterMode(boolean register) {
        registerMode = register;
        confirmBox.setVisible(register);
        confirmBox.setManaged(register);
        submit.setText(register ? "Create account" : "Sign in");
        confirm.clear();
    }

    private void submit() {
        String name = username.getText() == null ? "" : username.getText().trim();
        String pass = password.getText() == null ? "" : password.getText();

        if (name.isEmpty() || pass.isEmpty()) {
            alert("Missing fields", "Enter a username and a password.");
            return;
        }
        if (registerMode && !pass.equals(confirm.getText())) {
            alert("Passwords do not match", "Repeat the password: both fields must contain the same value.");
            return;
        }

        boolean register = registerMode;
        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return register ? auth.register(name, pass) : auth.login(name, pass);
            }
        };
        submit.setDisable(true);
        task.setOnSucceeded(e -> {
            submit.setDisable(false);
            password.clear();
            confirm.clear();
            onSuccess.accept(task.getValue(), register);
        });
        task.setOnFailed(e -> {
            submit.setDisable(false);
            password.clear();
            confirm.clear();
            password.requestFocus();
            alert(register ? "Registration failed" : "Sign-in failed", describe(task.getException()));
        });
        Thread worker = new Thread(task, "nexus-auth");
        worker.setDaemon(true);
        worker.start();
    }

    static String describe(Throwable error) {
        if (error instanceof AuthException failure) {
            return switch (failure.getReason()) {
                case INVALID_USERNAME -> "Username must be 3-30 characters: "
                        + "letters, digits, '_', '.' and '-'.";
                case WEAK_PASSWORD -> "Password must be " + AuthService.MIN_PASSWORD_LENGTH
                        + "-" + AuthService.MAX_PASSWORD_LENGTH + " characters long.";
                case USER_EXISTS -> "This username is already taken. Choose another one or sign in.";
                case USER_NOT_FOUND -> "User not found. Check the username or sign up.";
                case WRONG_PASSWORD -> "Wrong password.";
                case NO_PASSWORD_SET -> "This account has no password (it was created by an old version). "
                        + "Please register a new account.";
            };
        }
        if (error instanceof DataAccessException) {
            return "Database error: " + error.getMessage();
        }
        return "Unexpected error: " + (error == null ? "unknown" : error.getMessage());
    }

    private void alert(String title, String message) {
        Dialogs.error(getScene() == null ? null : getScene().getWindow(), title, message);
    }

    private static ToggleButton modeButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.getStyleClass().add("seg-button");
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
        return button;
    }

    private static VBox field(String caption, javafx.scene.Node control) {
        return new VBox(6, Ui.label(caption, "field-label"), control);
    }
}
