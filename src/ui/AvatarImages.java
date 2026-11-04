package ui;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import model.Rank;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AvatarImages {
    private static final double RING = 3;
    private static final double DECODE_SIZE = 512;
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private AvatarImages() {
    }

    static Image load(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            return null;
        }
        String key = path + "|" + file.lastModified();
        Image cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Image image = new Image(file.toURI().toString(), DECODE_SIZE, DECODE_SIZE, true, true, false);
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }
        CACHE.put(key, image);
        return image;
    }

    static Region circle(Image image, double size, Rank rank) {
        double inner = size - 2 * RING;
        double side = Math.min(image.getWidth(), image.getHeight());

        ImageView view = new ImageView(image);
        view.setViewport(new Rectangle2D((image.getWidth() - side) / 2, (image.getHeight() - side) / 2, side, side));
        view.setFitWidth(inner);
        view.setFitHeight(inner);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setClip(new Circle(inner / 2, inner / 2, inner / 2));

        StackPane pane = new StackPane(view);
        pane.getStyleClass().addAll("avatar", "avatar-photo", Ui.rankClass(rank));
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setAlignment(Pos.CENTER);
        return pane;
    }
}
