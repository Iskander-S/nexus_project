package ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.User;
import service.LocalAvatarStorage;

import java.io.File;
import java.nio.file.Path;

public final class ProfileDialog extends Dialog<ProfileEdit> {
    private static final double PREVIEW_SIZE = 96;

    private final User user;
    private final TextField name = new TextField();
    private final StackPane preview = new StackPane();
    private final ObjectProperty<Path> chosenFile = new SimpleObjectProperty<>();
    private final BooleanProperty removeRequested = new SimpleBooleanProperty(false);

    public ProfileDialog(Window owner, User user) {
        this.user = user;
        String currentName = user.getName();

        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(cancelType, saveType);

        Ui.limitLength(name, 30);
        name.setText(currentName);
        name.setPromptText("Your name");
        name.textProperty().addListener((obs, oldText, newText) -> refreshPreview());

        Label nameHint = Ui.label("3-30 characters: letters, digits, '_', '.', '-'.", "dialog-text");
        nameHint.setWrapText(true);

        Button upload = new Button("", Ui.icon(Ui.ICON_ADD, 13, 13));
        upload.getStyleClass().add("ghost");
        upload.textProperty().bind(Bindings.createStringBinding(
                () -> hasVisibleAvatar() ? "Change Avatar" : "Upload Avatar",
                chosenFile, removeRequested));
        upload.setOnAction(e -> chooseFile());

        Button remove = new Button("Remove", Ui.icon(Ui.ICON_TRASH, 13, 13));
        remove.getStyleClass().add("ghost");
        remove.setOnAction(e -> removeAvatar());
        remove.visibleProperty().bind(Bindings.createBooleanBinding(this::hasVisibleAvatar, chosenFile, removeRequested));
        remove.managedProperty().bind(remove.visibleProperty());

        Label avatarHint = Ui.label("PNG, JPG or JPEG, up to " + LocalAvatarStorage.MAX_BYTES / (1024 * 1024) + " MB.",
                "dialog-text");
        avatarHint.setWrapText(true);

        HBox avatarRow = new HBox(18, preview, new VBox(8, upload, remove, avatarHint));
        avatarRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(16,
                Ui.label("Edit profile", "dialog-title"),
                new VBox(8, Ui.label("Avatar", "field-label"), avatarRow),
                new VBox(6, Ui.label("Name", "field-label"), name),
                nameHint);
        content.setPadding(new Insets(0, 0, 4, 0));
        getDialogPane().setContent(content);

        Dialogs.style(this, owner, 500);
        refreshPreview();

        getDialogPane().lookupButton(saveType).disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String value = name.getText() == null ? "" : name.getText().trim();
            boolean unchanged = value.equals(currentName) && chosenFile.get() == null && !removeRequested.get();
            return value.isEmpty() || unchanged;
        }, name.textProperty(), chosenFile, removeRequested));

        setResultConverter(button -> button == saveType
                ? new ProfileEdit(name.getText().trim(), chosenFile.get(), removeRequested.get())
                : null);

        setOnShown(e -> {
            name.requestFocus();
            name.selectAll();
        });
    }

    private boolean hasVisibleAvatar() {
        return chosenFile.get() != null || (user.hasAvatar() && !removeRequested.get());
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose avatar image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images (PNG, JPG, JPEG)",
                "*.png", "*.jpg", "*.jpeg", "*.PNG", "*.JPG", "*.JPEG"));
        Window window = getDialogPane().getScene() == null ? null : getDialogPane().getScene().getWindow();
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            chosenFile.set(file.toPath());
            removeRequested.set(false);
            refreshPreview();
        }
    }

    private void removeAvatar() {
        chosenFile.set(null);
        removeRequested.set(user.hasAvatar());
        refreshPreview();
    }

    private void refreshPreview() {
        String shownName = name.getText() == null || name.getText().isBlank() ? user.getName() : name.getText();
        String path = chosenFile.get() != null
                ? chosenFile.get().toString()
                : removeRequested.get() ? null : user.getAvatarPath();
        Node avatar = Ui.avatar(shownName, path, PREVIEW_SIZE, user.getRank());
        preview.getChildren().setAll(avatar);
    }
}
