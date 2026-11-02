package ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Optional;

public final class Dialogs {
    private Dialogs() {
    }

    public static void style(Dialog<?> dialog, Window owner, double width) {
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.initStyle(StageStyle.UNDECORATED);

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(Ui.STYLESHEET);
        pane.getStyleClass().add("nx-dialog");
        pane.setPrefWidth(width);

        for (ButtonType type : pane.getButtonTypes()) {
            Node button = pane.lookupButton(type);
            if (button != null && type.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                button.getStyleClass().add("primary");
            }
        }
    }

    public static boolean confirm(Window owner, String title, String message, String confirmText) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType ok = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancel, ok);

        var text = Ui.label(message, "dialog-text");
        text.setWrapText(true);
        dialog.getDialogPane().setContent(new VBox(10, Ui.label(title, "dialog-title"), text));

        style(dialog, owner, 420);
        Node okButton = dialog.getDialogPane().lookupButton(ok);
        okButton.getStyleClass().remove("primary");
        okButton.getStyleClass().add("danger-solid");

        return dialog.showAndWait().filter(button -> button == ok).isPresent();
    }

    public static String askName(String defaultName) {
        Dialog<String> dialog = new Dialog<>();
        ButtonType begin = new ButtonType("Begin", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(begin);

        TextField field = new TextField();
        field.setPromptText("Your name");
        Ui.limitLength(field, 40);

        var hint = Ui.label("The system has detected a new player. Choose the name you will be known by.", "dialog-text");
        hint.setWrapText(true);
        dialog.getDialogPane().setContent(new VBox(12, Ui.label("Welcome to Nexus", "dialog-title"), hint, field));

        style(dialog, null, 440);
        dialog.getDialogPane().lookupButton(begin).disableProperty()
                .bind(Bindings.createBooleanBinding(() -> field.getText().isBlank(), field.textProperty()));
        dialog.setResultConverter(button -> button == begin ? field.getText().trim() : null);
        Platform.runLater(field::requestFocus);

        Optional<String> name = dialog.showAndWait();
        return name.filter(n -> !n.isBlank()).orElse(defaultName);
    }

    public static void error(Window owner, String title, String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.OK_DONE));

        var text = Ui.label(message, "dialog-text");
        text.setWrapText(true);
        dialog.getDialogPane().setContent(new VBox(10, Ui.label(title, "dialog-title"), text));

        style(dialog, owner, 520);
        dialog.showAndWait();
    }
}
