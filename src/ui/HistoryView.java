package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.GlobalQuest;
import model.Task;

import java.util.Locale;

public final class HistoryView extends ScrollPane {
    private final VBox list = new VBox(10);
    private final Label subtitle = Ui.label("", "page-subtitle");
    private final TextField search = new TextField();
    private final ObservableList<Task> source = FXCollections.observableArrayList();
    private final FilteredList<Task> filtered = new FilteredList<>(source, task -> true);

    public HistoryView() {
        search.setPromptText("Search history…");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((obs, oldText, newText) -> {
            filtered.setPredicate(task -> matches(task, newText));
            rebuild();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, new VBox(2, Ui.label("History", "page-title"), subtitle), spacer, search);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(18, header, list);
        page.setPadding(new Insets(32));
        page.getStyleClass().add("page");

        setContent(page);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
    }

    public void render(Snapshot snapshot) {
        source.setAll(snapshot.completed());
        rebuild();
    }

    private void rebuild() {
        int total = source.size();
        int baseXp = source.stream().mapToInt(Task::getBaseXp).sum();
        boolean searching = !query().isEmpty();

        if (total == 0) {
            subtitle.setText("Completed quests will appear here.");
        } else if (searching) {
            subtitle.setText(filtered.size() + " of " + total + " shown");
        } else {
            subtitle.setText(total + " completed  ·  " + baseXp + " base XP");
        }

        list.getChildren().clear();
        if (total == 0) {
            list.getChildren().add(emptyState("Nothing here yet", "Finish a quest and it will be recorded in your history."));
            return;
        }
        if (filtered.isEmpty()) {
            list.getChildren().add(emptyState("No quests match “" + search.getText().trim() + "”",
                    "Check the spelling or clear the search field."));
            return;
        }
        for (Task task : filtered) {
            list.getChildren().add(row(task));
        }
    }

    private String query() {
        return search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(Task task, String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        return contains(task.getTitle(), query)
                || contains(task.getDescription(), query)
                || contains(QuestKind.of(task).label(), query)
                || (task.getGrade() != null && task.getGrade().name().toLowerCase(Locale.ROOT).equals(query));
    }

    private static boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private static Region emptyState(String titleText, String hintText) {
        VBox box = new VBox(10, Ui.label(titleText, "empty-title"), Ui.label(hintText, "empty-hint"));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state");
        return box;
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
        if (task.getGrade() != null) {
            row.getChildren().add(Ui.gradeChip(task.getGrade()));
        }
        row.getChildren().add(Ui.chip("+" + task.getBaseXp() + " XP", "xp"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("card", "history-row");
        return row;
    }
}
