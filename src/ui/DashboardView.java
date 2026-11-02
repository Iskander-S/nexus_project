package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.GlobalQuest;
import model.Rank;
import model.Task;
import model.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class DashboardView extends ScrollPane {
    private final QuestActions actions;

    private final Label greeting = Ui.label("", "page-title");
    private final StackPane avatarSlot = new StackPane();
    private final Label nameLabel = Ui.label("", "hero-name");
    private final HBox badges = new HBox(10);
    private final Label levelValue = Ui.label("", "bar-value");
    private final XpBar levelBar = new XpBar();
    private final Label rankCaption = Ui.label("", "bar-caption");
    private final Label rankValue = Ui.label("", "bar-value");
    private final XpBar rankBar = new XpBar("rank-fill");

    private final Label activeValue = Ui.label("0", "stat-value");
    private final Label completedValue = Ui.label("0", "stat-value");
    private final Label totalXpValue = Ui.label("0", "stat-value");
    private final Label availableValue = Ui.label("0", "stat-value");

    private final VBox topQuests = new VBox(10);

    public DashboardView(QuestActions actions) {
        this.actions = actions;

        Label subtitle = Ui.label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), "page-subtitle");

        VBox identity = new VBox(8, nameLabel, badges);
        identity.setAlignment(Pos.CENTER_LEFT);
        avatarSlot.setMinSize(84, 84);

        HBox levelHeader = new HBox(Ui.label("Level progress", "bar-caption"), spacer(), levelValue);
        HBox rankHeader = new HBox(rankCaption, spacer(), rankValue);
        VBox.setMargin(rankHeader, new Insets(10, 0, 0, 0));
        VBox bars = new VBox(8, levelHeader, levelBar, rankHeader, rankBar);

        HBox heroTop = new HBox(20, avatarSlot, identity);
        heroTop.setAlignment(Pos.CENTER_LEFT);
        VBox hero = new VBox(22, heroTop, bars);
        hero.getStyleClass().addAll("card", "hero-card");

        HBox stats = new HBox(16,
                tile(activeValue, "Active quests"),
                tile(completedValue, "Completed"),
                tile(totalXpValue, "Total XP"),
                tile(availableValue, "XP up for grabs"));

        Button newQuest = new Button("New quest", Ui.icon(Ui.ICON_ADD, 13, 13));
        newQuest.getStyleClass().add("primary");
        newQuest.setOnAction(e -> actions.addQuest());
        HBox topHeader = new HBox(Ui.label("Top quests", "section-title"), spacer(), newQuest);
        topHeader.setAlignment(Pos.CENTER_LEFT);
        VBox topCard = new VBox(14, topHeader, topQuests);
        topCard.getStyleClass().add("card");

        VBox page = new VBox(22, new VBox(2, greeting, subtitle), hero, stats, topCard);
        page.setPadding(new Insets(32));
        page.getStyleClass().add("page");

        setContent(page);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
    }

    public void render(Snapshot snapshot) {
        User user = snapshot.user();
        List<Task> active = snapshot.active();

        greeting.setText(greetingFor(LocalTime.now().getHour()) + ", " + user.getName());
        avatarSlot.getChildren().setAll(Ui.avatar(user.getName(), 84, user.getRank()));
        nameLabel.setText(user.getName());
        badges.getChildren().setAll(Ui.rankBadge(user.getRank()), Ui.chip("Level " + user.getLevel(), "level"));

        levelValue.setText(user.getXpIntoLevel() + " / " + User.XP_PER_LEVEL + " XP");
        levelBar.setProgress(user.getXpIntoLevel() / (double) User.XP_PER_LEVEL);

        Rank rank = user.getRank();
        Optional<Rank> next = rank.next();
        if (next.isPresent()) {
            int span = next.get().getMinXp() - rank.getMinXp();
            int into = user.getCurrentXp() - rank.getMinXp();
            rankCaption.setText("Next rank: " + next.get().getDisplayName());
            rankValue.setText(into + " / " + span + " XP");
            rankBar.setProgress(into / (double) span);
        } else {
            rankCaption.setText("Maximum rank reached");
            rankValue.setText(user.getCurrentXp() + " XP");
            rankBar.setProgress(1);
        }

        activeValue.setText(String.valueOf(active.size()));
        completedValue.setText(String.valueOf(snapshot.completed().size()));
        totalXpValue.setText(String.valueOf(user.getCurrentXp()));
        availableValue.setText(String.valueOf(active.stream().mapToInt(Task::getBaseXp).sum()));

        topQuests.getChildren().clear();
        List<Task> top = active.stream()
                .sorted(Comparator.comparingInt((Task t) -> t.getBaseXp()).reversed())
                .limit(4)
                .toList();
        if (top.isEmpty()) {
            topQuests.getChildren().add(Ui.label("No active quests yet. Create one and start climbing the ranks.", "empty-hint"));
        }
        for (Task task : top) {
            topQuests.getChildren().add(row(task));
        }
    }

    private Region row(Task task) {
        QuestKind kind = QuestKind.of(task);
        Label title = Ui.label(task.getTitle(), "row-title");
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        HBox row = new HBox(12, Ui.chip(kind.label(), kind.css()), title);
        if (task instanceof GlobalQuest quest) {
            row.getChildren().add(Ui.chip("Target " + quest.getTargetScore(), "target"));
        }
        row.getChildren().add(Ui.chip("+" + task.getBaseXp() + " XP", "xp"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("row");
        return row;
    }

    private static VBox tile(Label value, String caption) {
        VBox tile = new VBox(4, value, Ui.label(caption, "stat-label"));
        tile.getStyleClass().addAll("card", "stat-tile");
        tile.setPrefWidth(100);
        HBox.setHgrow(tile, Priority.ALWAYS);
        return tile;
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    static String greetingFor(int hour) {
        if (hour < 5) return "Good night";
        if (hour < 12) return "Good morning";
        if (hour < 18) return "Good afternoon";
        return "Good evening";
    }
}
