package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.GlobalQuest;
import model.Task;
import java.util.List;

public final class HistoryView extends ScrollPane {
    private final VBox list = new VBox(10);
    private final Label subtitle = Ui.label("", "page-subtitle");

    public HistoryView() {
        VBox page = new VBox(18, new VBox(2, Ui.label("History", "page-title"), subtitle), list);
        page.setPadding(new Insets(32));
        page.getStyleClass().add("page");

        setContent(page);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
    }

    public void render(Snapshot snapshot) {
        List<Task> done = snapshot.completed();
        int baseXp = done.stream().mapToInt(Task::getBaseXp).sum();
        subtitle.setText(done.isEmpty() ? "Completed quests will appear here."
                : done.size() + " completed  ·  " + baseXp + " base XP");

        list.getChildren().clear();
        if (done.isEmpty()) {
            Label title = Ui.label("Nothing here yet", "empty-title");
            Label hint = Ui.label("Finish a quest and it will be recorded in your history.", "empty-hint");
            VBox box = new VBox(10, title, hint);
            box.setAlignment(Pos.CENTER);
            box.getStyleClass().add("empty-state");
            list.getChildren().add(box);
            return;
        }
        for (Task task : done) {
            list.getChildren().add(row(task));
        }
    }

    private Region row(Task task) {
        QuestKind kind = QuestKind.of(task);

        StackPane mark = new StackPane(Ui.icon(Ui.ICON_CHECK, 14, 11));
        mark.getStyleClass().add("done-mark");
        mark.setMinSize(30, 30);
        mark.setMaxSize(30, 30);

        Label title = Ui.label(task.getTitle(), "row-title");
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        HBox row = new HBox(14, mark, title, Ui.chip(kind.label(), kind.css()));
        if (task instanceof GlobalQuest quest) {
            row.getChildren().add(Ui.chip("Target " + quest.getTargetScore(), "target"));
        }
        row.getChildren().add(Ui.chip("+" + task.getBaseXp() + " XP", "xp"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("card", "history-row");
        return row;
    }
}
