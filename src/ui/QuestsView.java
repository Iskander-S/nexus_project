package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.GlobalQuest;
import model.Task;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuestsView extends ScrollPane {
    private final QuestActions actions;
    private final VBox list = new VBox(12);
    private final ToggleButton allFilter = filterButton("All", null);
    private final Map<QuestKind, ToggleButton> kindFilters = new EnumMap<>(QuestKind.class);
    private final Label subtitle = Ui.label("", "page-subtitle");
    private final TextField search = new TextField();

    private List<Task> active = List.of();
    private QuestKind selected;

    public QuestsView(QuestActions actions) {
        this.actions = actions;

        ToggleGroup group = new ToggleGroup();
        HBox filters = new HBox(8);
        allFilter.setToggleGroup(group);
        filters.getChildren().add(allFilter);
        for (QuestKind kind : QuestKind.values()) {
            ToggleButton button = filterButton(kind.label(), kind);
            button.setToggleGroup(group);
            kindFilters.put(kind, button);
            filters.getChildren().add(button);
        }
        group.selectToggle(allFilter);
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                group.selectToggle(oldToggle);
                return;
            }
            selected = (QuestKind) newToggle.getUserData();
            rebuild();
        });

        Button newQuest = new Button("New quest", Ui.icon(Ui.ICON_ADD, 13, 13));
        newQuest.getStyleClass().add("primary");
        newQuest.setOnAction(e -> actions.addQuest());
        Tooltip.install(newQuest, new Tooltip("Ctrl+N"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(new VBox(2, Ui.label("Quests", "page-title"), subtitle), spacer, newQuest);
        header.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("Search quests…");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((obs, oldText, newText) -> rebuild());

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);
        HBox filterRow = new HBox(12, filters, filterSpacer, search);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(18, header, filterRow, list);
        page.setPadding(new Insets(32));
        page.getStyleClass().add("page");

        setContent(page);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
    }

    public void render(Snapshot snapshot) {
        active = snapshot.active();
        subtitle.setText(active.isEmpty() ? "Nothing on your plate right now." : active.size() + " in progress");

        allFilter.setText("All · " + active.size());
        for (QuestKind kind : QuestKind.values()) {
            long count = active.stream().filter(t -> QuestKind.of(t) == kind).count();
            kindFilters.get(kind).setText(kind.label() + " · " + count);
        }
        rebuild();
    }

    private void rebuild() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<Task> visible = active.stream()
                .filter(t -> selected == null || QuestKind.of(t) == selected)
                .filter(t -> query.isEmpty() || t.getTitle().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Task.DEADLINE_ORDER)
                .toList();

        list.getChildren().clear();
        if (visible.isEmpty()) {
            list.getChildren().add(emptyState());
            return;
        }
        for (Task task : visible) {
            list.getChildren().add(card(task));
        }
    }

    private Node card(Task task) {
        QuestKind kind = QuestKind.of(task);

        Region stripe = new Region();
        stripe.getStyleClass().addAll("stripe", "stripe-" + kind.css());
        stripe.setMinWidth(5);
        stripe.setPrefWidth(5);
        stripe.setMaxWidth(5);

        Label title = Ui.label(task.getTitle(), "quest-title");
        title.setWrapText(true);
        VBox text = new VBox(6, title);
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            Label description = Ui.label(task.getDescription(), "quest-desc");
            description.setWrapText(true);
            text.getChildren().add(description);
        }
        HBox chips = new HBox(8, Ui.chip(kind.label(), kind.css()), Ui.chip("+" + task.getBaseXp() + " XP", "xp"));
        if (task instanceof GlobalQuest quest) {
            chips.getChildren().add(Ui.chip("Target " + quest.getTargetScore(), "target"));
        }
        Label deadline = Ui.deadlineChip(task);
        if (deadline != null) {
            chips.getChildren().add(deadline);
        }
        text.getChildren().add(chips);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button complete = new Button("Complete", Ui.icon(Ui.ICON_CHECK, 14, 11));
        complete.getStyleClass().add("primary");
        complete.setOnAction(e -> actions.completeQuest(task));

        Button delete = new Button();
        delete.setGraphic(Ui.icon(Ui.ICON_TRASH, 13, 17));
        delete.getStyleClass().addAll("icon-button", "danger");
        delete.setTooltip(new Tooltip("Delete quest"));
        delete.setOnAction(e -> actions.deleteQuest(task));

        HBox buttons = new HBox(8, complete, delete);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        HBox body = new HBox(16, text, buttons);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setPadding(new Insets(16, 18, 16, 18));
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox card = new HBox(stripe, body);
        card.getStyleClass().add("quest-card");
        if (Ui.isOverdue(task)) {
            card.getStyleClass().add("quest-card-overdue");
        }
        return card;
    }

    private Node emptyState() {
        String query = search.getText() == null ? "" : search.getText().trim();
        boolean searching = !query.isEmpty() && !active.isEmpty();
        boolean filtered = selected != null && !active.isEmpty();
        String titleText = searching ? "No quests match “" + query + "”"
                : filtered ? "No " + selected.label().toLowerCase() + " quests" : "No active quests";
        String hintText = searching ? "Check the spelling or clear the search field."
                : filtered ? "Try another filter, or add a new quest of this type."
                : "Create your first quest and start earning XP.";
        Label title = Ui.label(titleText, "empty-title");
        Label hint = Ui.label(hintText, "empty-hint");
        Button add = new Button("New quest", Ui.icon(Ui.ICON_ADD, 13, 13));
        add.getStyleClass().add("primary");
        add.setOnAction(e -> actions.addQuest());

        VBox box = new VBox(10, title, hint, add);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state");
        return box;
    }

    private static ToggleButton filterButton(String text, QuestKind kind) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(kind);
        button.getStyleClass().add("filter-button");
        return button;
    }
}
