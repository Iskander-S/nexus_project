package ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public final class XpBar extends StackPane {
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private Timeline timeline;

    public XpBar(String... extraFillClasses) {
        getStyleClass().add("xp-track");
        setMinHeight(12);
        setPrefHeight(12);
        setMaxHeight(12);

        Region fill = new Region();
        fill.getStyleClass().add("xp-fill");
        fill.getStyleClass().addAll(extraFillClasses);
        fill.setMinWidth(0);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        fill.maxWidthProperty().bind(widthProperty().multiply(progress));
        fill.prefWidthProperty().bind(fill.maxWidthProperty());
        getChildren().add(fill);
    }

    public void setProgress(double target) {
        double clamped = Math.max(0, Math.min(1, target));
        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline(new KeyFrame(Duration.millis(650),
                new KeyValue(progress, clamped, Interpolator.EASE_BOTH)));
        timeline.play();
    }
}
