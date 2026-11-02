package ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import model.DailyTask;
import model.GlobalQuest;
import model.Task;
import model.WeeklyTask;

import java.util.UUID;

public final class AddQuestDialog extends Dialog<Task> {
    private QuestKind kind = QuestKind.DAILY;

    public AddQuestDialog(Window owner) {
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType addType = new ButtonType("Add quest", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(cancelType, addType);

        ToggleGroup group = new ToggleGroup();
        HBox segmented = new HBox(8);
        for (QuestKind k : QuestKind.values()) {
            ToggleButton button = new ToggleButton(k.label());
            button.setUserData(k);
            button.setToggleGroup(group);
            button.getStyleClass().add("seg-button");
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
            segmented.getChildren().add(button);
        }
        Label kindHint = Ui.label(kind.hint(), "dialog-text");
        kindHint.setWrapText(true);

        TextField title = new TextField();
        title.setPromptText("What needs to be done?");
        Ui.limitLength(title, 100);

        TextArea description = new TextArea();
        description.setPromptText("Details (optional)");
        description.setPrefRowCount(3);
        description.setWrapText(true);
        Ui.limitLength(description, 500);

        Spinner<Integer> xp = Ui.intSpinner(1, 10_000, kind.defaultXp());
        Spinner<Integer> target = Ui.intSpinner(1, 100, 90);

        VBox xpBox = field("Base XP", xp);
        VBox targetBox = field("Target score (1–100)", target);
        HBox.setHgrow(xpBox, Priority.ALWAYS);
        HBox.setHgrow(targetBox, Priority.ALWAYS);
        targetBox.setVisible(false);
        targetBox.setManaged(false);

        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {          // нельзя снять выбор — тип всегда задан
                group.selectToggle(oldToggle);
                return;
            }
            kind = (QuestKind) newToggle.getUserData();
            kindHint.setText(kind.hint());
            xp.getValueFactory().setValue(kind.defaultXp());
            targetBox.setVisible(kind == QuestKind.GLOBAL);
            targetBox.setManaged(kind == QuestKind.GLOBAL);
        });
        group.selectToggle(group.getToggles().get(0));

        VBox content = new VBox(14,
                Ui.label("New quest", "dialog-title"),
                segmented,
                kindHint,
                field("Title", title),
                field("Description", description),
                new HBox(14, xpBox, targetBox));
        content.setPadding(new Insets(0, 0, 4, 0));
        getDialogPane().setContent(content);

        Dialogs.style(this, owner, 500);
        getDialogPane().lookupButton(addType).disableProperty()
                .bind(Bindings.createBooleanBinding(() -> title.getText().isBlank(), title.textProperty()));

        setResultConverter(button -> {
            if (button != addType) {
                return null;
            }
            Ui.commit(xp);
            Ui.commit(target);
            String id = "T-" + UUID.randomUUID().toString().substring(0, 8);
            String name = title.getText().trim();
            String details = description.getText().trim();
            int baseXp = xp.getValue();
            return switch (kind) {
                case DAILY -> new DailyTask(id, name, details, baseXp);
                case WEEKLY -> new WeeklyTask(id, name, details, baseXp);
                case GLOBAL -> new GlobalQuest(id, name, details, baseXp, target.getValue());
            };
        });

        setOnShown(e -> title.requestFocus());
    }

    private static VBox field(String caption, javafx.scene.Node control) {
        return new VBox(6, Ui.label(caption, "field-label"), control);
    }
}
