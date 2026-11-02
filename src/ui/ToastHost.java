package ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class ToastHost extends VBox {

    public enum Kind {
        INFO("toast-info", 3000), SUCCESS("toast-success", 3200), ERROR("toast-error", 5500), LEVEL_UP("toast-levelup", 4200);

        private final String css;
        private final int millis;

        Kind(String css, int millis) {
            this.css = css;
            this.millis = millis;
        }
    }

    private static final int MAX_VISIBLE = 4;

    public ToastHost() {
        setSpacing(10);
        setMouseTransparent(true);
        setPickOnBounds(false);
    }

    public void show(String message, Kind kind) {
        Label toast = Ui.label(message, "toast", kind.css);
        toast.setWrapText(true);
        toast.setMaxWidth(360);
        toast.setOpacity(0);

        getChildren().add(toast);
        while (getChildren().size() > MAX_VISIBLE) {
            getChildren().remove(0);
        }

        FadeTransition in = new FadeTransition(Duration.millis(180), toast);
        in.setToValue(1);
        in.play();

        PauseTransition wait = new PauseTransition(Duration.millis(kind.millis));
        wait.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(300), toast);
            out.setToValue(0);
            out.setOnFinished(done -> getChildren().remove(toast));
            out.play();
        });
        wait.play();
    }
}
