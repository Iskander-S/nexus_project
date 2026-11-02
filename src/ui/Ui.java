package ui;

import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import model.PerformanceGrade;
import model.Rank;
import java.util.Objects;

public final class Ui {
    private Ui() {
    }

    public static final String STYLESHEET = Objects
            .requireNonNull(Ui.class.getResource("/css/nexus.css"), "css/nexus.css not found on classpath")
            .toExternalForm();

    public static final String ICON_BRAND = "M12 2 L22 12 L12 22 L2 12 Z M12 7 L7 12 L12 17 L17 12 Z";
    public static final String ICON_DASHBOARD = "M3 3 H11 V11 H3 Z M13 3 H21 V11 H13 Z M3 13 H11 V21 H3 Z M13 13 H21 V21 H13 Z";
    public static final String ICON_QUESTS = "M4 5 H20 V7 H4 Z M4 11 H20 V13 H4 Z M4 17 H14 V19 H4 Z";
    public static final String ICON_CHECK = "M9 16.2 L4.8 12 L3.4 13.4 L9 19 L21 7 L19.6 5.6 Z";
    public static final String ICON_ADD = "M19 13 H13 V19 H11 V13 H5 V11 H11 V5 H13 V11 H19 Z";
    public static final String ICON_TRASH = "M6 19 C6 20.1 6.9 21 8 21 H16 C17.1 21 18 20.1 18 19 V7 H6 Z "
            + "M19 4 H15.5 L14.5 3 H9.5 L8.5 4 H5 V6 H19 Z";

    public static Region icon(String svgPath, double width, double height) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgPath);
        Region region = new Region();
        region.setShape(svg);
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
        region.getStyleClass().add("icon");
        return region;
    }

    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    public static Label chip(String text, String variant) {
        return label(text, "chip", "chip-" + variant);
    }

    public static Label rankBadge(Rank rank) {
        return label(rank.getDisplayName(), "rank-badge", rankClass(rank));
    }

    public static Label avatar(String name, double size, Rank rank) {
        Label avatar = label(initial(name), "avatar", rankClass(rank));
        avatar.setMinSize(size, size);
        avatar.setPrefSize(size, size);
        avatar.setMaxSize(size, size);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-font-size: " + Math.round(size * 0.42) + "px;");
        return avatar;
    }

    public static String initial(String name) {
        if (name == null || name.isBlank()) return "?";
        String trimmed = name.trim();
        return new String(Character.toChars(trimmed.codePointAt(0))).toUpperCase();
    }

    public static String rankClass(Rank rank) {
        return "rank-" + rank.name().substring(0, 1).toLowerCase();
    }

    public static String gradeClass(PerformanceGrade grade) {
        return "grade-" + grade.name().toLowerCase();
    }

    public static void limitLength(TextInputControl control, int maxLength) {
        control.setTextFormatter(new TextFormatter<String>(
                change -> change.getControlNewText().length() <= maxLength ? change : null));
    }

    public static Spinner<Integer> intSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.getEditor().focusedProperty().addListener((obs, was, isFocused) -> {
            if (!isFocused) commit(spinner);
        });
        return spinner;
    }

    public static void commit(Spinner<Integer> spinner) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
        int value;
        try {
            int parsed = Integer.parseInt(spinner.getEditor().getText().trim());
            value = Math.max(factory.getMin(), Math.min(factory.getMax(), parsed));
        } catch (NumberFormatException e) {
            value = spinner.getValue();
        }
        factory.setValue(value);
        spinner.getEditor().setText(String.valueOf(value));
    }
}
