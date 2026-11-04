package ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class ToastHost extends VBox implements NotificationService {
    public static final int VISIBLE_MILLIS = 3000;

    private static final int FADE_IN_MILLIS = 180;
    private static final int FADE_OUT_MILLIS = 300;
    private static final int MAX_VISIBLE = 4;

    public ToastHost() {
        setSpacing(10);
        setMouseTransparent(true);
        setPickOnBounds(false);
    }

    @Override
    public void show(String message, Kind kind) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(message, kind));
            return;
        }

        Label toast = Ui.label(message, "toast", cssOf(kind));
        toast.setWrapText(true);
        toast.setMaxWidth(360);
        toast.setOpacity(0);

        getChildren().add(toast);
        while (getChildren().size() > MAX_VISIBLE) {
            getChildren().remove(0);
        }

        FadeTransition in = new FadeTransition(Duration.millis(FADE_IN_MILLIS), toast);
        in.setToValue(1);

        PauseTransition hold = new PauseTransition(
                Duration.millis(VISIBLE_MILLIS - FADE_IN_MILLIS - FADE_OUT_MILLIS));

        FadeTransition out = new FadeTransition(Duration.millis(FADE_OUT_MILLIS), toast);
        out.setToValue(0);

        SequentialTransition lifecycle = new SequentialTransition(in, hold, out);
        lifecycle.setOnFinished(e -> getChildren().remove(toast));
        lifecycle.play();
    }

    private static String cssOf(Kind kind) {
        return switch (kind) {
            case INFO -> "toast-info";
            case SUCCESS -> "toast-success";
            case ERROR -> "toast-error";
            case LEVEL_UP -> "toast-levelup";
            case ACHIEVEMENT -> "toast-achievement";
        };
    }
}
