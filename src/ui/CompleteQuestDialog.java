package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import model.GlobalQuest;
import model.PerformanceGrade;

public final class CompleteQuestDialog extends Dialog<Integer> {
    private final GlobalQuest quest;
    private final Label gradeBadge = Ui.label("", "grade-badge");
    private final Label xpLabel = Ui.label("", "dialog-text");

    public CompleteQuestDialog(Window owner, GlobalQuest quest) {
        this.quest = quest;

        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType submitType = new ButtonType("Submit result", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(cancelType, submitType);

        Slider slider = new Slider(0, 100, quest.getTargetScore());
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(4);
        slider.setShowTickMarks(true);
        slider.setBlockIncrement(1);
        HBox.setHgrow(slider, Priority.ALWAYS);

        Spinner<Integer> spinner = Ui.intSpinner(0, 100, quest.getTargetScore());
        spinner.setPrefWidth(96);
        spinner.setMaxWidth(96);

        slider.valueProperty().addListener((obs, oldValue, newValue) -> {
            int score = (int) Math.round(newValue.doubleValue());
            if (spinner.getValue() != score) {
                spinner.getValueFactory().setValue(score);
            }
        });
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) return;
            if (Math.round(slider.getValue()) != newValue) {
                slider.setValue(newValue);
            }
            update(newValue);
        });

        Label questTitle = Ui.label(quest.getTitle(), "quest-title");
        questTitle.setWrapText(true);

        HBox inputRow = new HBox(14, slider, spinner);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        HBox result = new HBox(16, gradeBadge, xpLabel);
        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add("result-box");

        VBox content = new VBox(14,
                Ui.label("Submit your result", "dialog-title"),
                questTitle,
                Ui.chip("Target score " + quest.getTargetScore(), "target"),
                Ui.label("Your score", "field-label"),
                inputRow,
                result);
        content.setPadding(new Insets(0, 0, 4, 0));
        getDialogPane().setContent(content);

        Dialogs.style(this, owner, 480);
        update(quest.getTargetScore());

        setResultConverter(button -> {
            if (button != submitType) {
                return null;
            }
            Ui.commit(spinner);
            return spinner.getValue();
        });
    }

    private void update(int score) {
        PerformanceGrade grade = quest.evaluatePerformance(score);
        int bonus = quest.calculateBonusXp(quest.getBaseXp(), grade);
        gradeBadge.setText(grade.name());
        gradeBadge.getStyleClass().setAll("grade-badge", Ui.gradeClass(grade));
        xpLabel.setText(quest.getBaseXp() + " base + " + bonus + " bonus = " + (quest.getBaseXp() + bonus) + " XP");
    }
}
